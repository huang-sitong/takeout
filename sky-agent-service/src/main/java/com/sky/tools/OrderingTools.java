package com.sky.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sky.context.BaseContext;
import com.sky.entity.Category;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.feign.CartFeignClient;
import com.sky.feign.MenuFeignClient;
import com.sky.feign.ShopFeignClient;
import com.sky.result.Result;
import com.sky.vo.menu.DishVO;import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 点餐助手工具集（Phase B Function Calling）
 *
 * 权限边界：仅覆盖「浏览菜单 + 管理购物车」，刻意不提供下单/地址簿工具——
 * 下单涉及支付等敏感操作，用户须在小程序购物车页面自行完成结算。
 *
 * 身份传递链路：Gateway 注入 X-User-Id → Controller 取出放入 ToolContext →
 * 工具方法 set 进 BaseContext → FeignInterceptor 回退分支读取并透传下游。
 * （SSE 流式场景工具执行不在 Tomcat 请求线程，RequestContextHolder 不可用，
 * 必须经 ToolContext 显式传递，且 Feign 拦截器在同一调用栈内执行，ThreadLocal 有效。）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderingTools {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static final String CTX_USER_ID = "userId";

    private final MenuFeignClient menuFeignClient;
    private final CartFeignClient cartFeignClient;
    private final ShopFeignClient shopFeignClient;

    // ==================== 菜单浏览 ====================

    @Tool(name = "getShopStatus", description = "查询店铺当前营业状态。推荐菜品前应先调用；已打烊时不推荐任何菜品")
    public String getShopStatus(ToolContext toolContext) {
        return withUser(toolContext, () -> {
            Integer status = unwrap(shopFeignClient.getShopStatus());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", status);
            m.put("meaning", status != null && status == 1 ? "营业中" : "已打烊");
            return m;
        });
    }

    @Tool(name = "getCategories", description = "查询菜品分类列表。type 可选：1=菜品分类，2=套餐分类，不传返回全部")
    public String getCategories(
            @ToolParam(description = "分类类型：1菜品分类 2套餐分类，可为空", required = false) Integer type,
            ToolContext toolContext) {
        return withUser(toolContext, () -> {
            List<Category> categories = unwrap(menuFeignClient.listCategories(type));
            List<Map<String, Object>> list = new ArrayList<>();
            for (Category c : categories) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.getId());
                m.put("name", c.getName());
                m.put("type", c.getType() == 1 ? "菜品" : "套餐");
                list.add(m);
            }
            return list;
        });
    }

    @Tool(name = "getDishes", description = "按分类查询【起售中】的菜品，含价格、简介和口味选项。需先通过 getCategories 获取 categoryId")
    public String getDishes(
            @ToolParam(description = "菜品分类id") Long categoryId,
            ToolContext toolContext) {
        return withUser(toolContext, () -> {
            List<DishVO> dishes = unwrap(menuFeignClient.listSellingDishByCategory(categoryId));
            List<Map<String, Object>> list = new ArrayList<>();
            for (DishVO d : dishes) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", d.getId());
                m.put("name", d.getName());
                m.put("price", d.getPrice());
                if (d.getDescription() != null && !d.getDescription().isBlank()) {
                    m.put("description", d.getDescription());
                }
                if (d.getFlavors() != null && !d.getFlavors().isEmpty()) {
                    List<String> flavorNames = d.getFlavors().stream()
                            .map(f -> f.getName()).filter(n -> n != null && !n.isBlank()).toList();
                    if (!flavorNames.isEmpty()) {
                        m.put("flavors", flavorNames); // 加购此类菜品必须先问用户口味
                    }
                }
                list.add(m);
            }
            return list;
        });
    }

    @Tool(name = "getSetmeals", description = "按分类查询【起售中】的套餐（含价格、简介）。套餐无口味选项，可直接加购")
    public String getSetmeals(
            @ToolParam(description = "套餐分类id") Long categoryId,
            ToolContext toolContext) {
        return withUser(toolContext, () -> {
            List<Setmeal> setmeals = unwrap(menuFeignClient.listSellingSetmealByCategory(categoryId));
            List<Map<String, Object>> list = new ArrayList<>();
            for (Setmeal s : setmeals) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("name", s.getName());
                m.put("price", s.getPrice());
                if (s.getDescription() != null && !s.getDescription().isBlank()) {
                    m.put("description", s.getDescription());
                }
                list.add(m);
            }
            return list;
        });
    }

    // ==================== 购物车操作（能力上限）====================

    @Tool(name = "getCart", description = "查看当前用户购物车的明细（商品名、口味、数量、金额）及合计金额")
    public String getCart(ToolContext toolContext) {
        return withUser(toolContext, () -> {
            List<ShoppingCart> items = unwrap(cartFeignClient.listByUserId());
            List<Map<String, Object>> list = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (ShoppingCart item : items) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", item.getName());
                if (item.getDishFlavor() != null && !item.getDishFlavor().isBlank()) {
                    m.put("flavor", item.getDishFlavor());
                }
                m.put("quantity", item.getNumber());
                m.put("amount", item.getAmount());
                list.add(m);
                total = total.add(item.getAmount() == null ? BigDecimal.ZERO
                        : item.getAmount().multiply(BigDecimal.valueOf(item.getNumber())));
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("items", list);
            result.put("totalAmount", total);
            return result;
        });
    }

    @Tool(name = "addToCart", description = "将一份菜品或套餐加入当前用户购物车（数量+1）。dishId 与 setmealId 必须二选一；带口味选项的菜品必须传用户明确选择的 dishFlavor")
    public String addToCart(
            @ToolParam(description = "菜品id（加购菜品时必填）", required = false) Long dishId,
            @ToolParam(description = "套餐id（加购套餐时必填）", required = false) Long setmealId,
            @ToolParam(description = "口味名称，如\"微辣\"；仅当菜品有口味选项时由用户指定", required = false) String dishFlavor,
            ToolContext toolContext) {
        if (dishId == null && setmealId == null) {
            return errorJson("dishId 与 setmealId 必须提供一个");
        }
        return withUser(toolContext, () -> {
            // 预校验：id 必须真实存在，防止 LLM 幻觉参数导致下游 500
            if (dishId != null) {
                DishVO dish = unwrap(menuFeignClient.getDishById(dishId));
                if (dish == null) {
                    return errorJson("dishId=" + dishId + " 不存在，请重新调用 getDishes 获取正确的菜品id后再加购");
                }
            }
            if (setmealId != null) {
                com.sky.vo.menu.SetmealVO setmeal = unwrap(menuFeignClient.getSetmealById(setmealId));
                if (setmeal == null) {
                    return errorJson("setmealId=" + setmealId + " 不存在，请重新调用 getSetmeals 获取正确的套餐id后再加购");
                }
            }
            ShoppingCart added = unwrap(cartFeignClient.add(buildCartDTO(dishId, setmealId, dishFlavor)));
            return added == null ? Map.of("message", "已加入购物车") : briefCartItem(added, "已加入购物车");
        });
    }

    @Tool(name = "removeFromCart", description = "从当前用户购物车减少一份商品（数量-1，减至0自动移除）。dishId 与 setmealId 必须二选一，口味须与加购时一致")
    public String removeFromCart(
            @ToolParam(description = "菜品id（移除菜品时必填）", required = false) Long dishId,
            @ToolParam(description = "套餐id（移除套餐时必填）", required = false) Long setmealId,
            @ToolParam(description = "口味名称，须与加购时一致", required = false) String dishFlavor,
            ToolContext toolContext) {
        if (dishId == null && setmealId == null) {
            return errorJson("dishId 与 setmealId 必须提供一个");
        }
        return withUser(toolContext, () -> {
            unwrap(cartFeignClient.sub(buildCartDTO(dishId, setmealId, dishFlavor)));
            return Map.of("message", "已从购物车减少一份");
        });
    }

    @Tool(name = "clearCart", description = "清空当前用户的整个购物车。仅在用户明确要求清空时调用")
    public String clearCart(ToolContext toolContext) {
        return withUser(toolContext, () -> {
            unwrap(cartFeignClient.cleanCart());
            return Map.of("message", "购物车已清空");
        });
    }

    // ==================== 内部辅助 ====================

    /**
     * 统一执行模板：校验 ToolContext 中的 userId → 写入 BaseContext → 执行 Feign 调用 →
     * 结果序列化为紧凑 JSON → finally 清理 ThreadLocal（防线程池复用泄漏）。
     */
    private String withUser(ToolContext toolContext, Supplier<Object> action) {
        Long userId = resolveUserId(toolContext);
        if (userId == null) {
            return errorJson("无法识别当前用户身份，请确认请求携带有效登录凭证");
        }
        BaseContext.setCurrentId(userId);
        try {
            return toJson(action.get());
        } catch (Exception e) {
            log.error("Agent tool failed: {}", e.getMessage(), e);
            return errorJson(e.getMessage() == null ? "服务暂时不可用，请稍后再试" : e.getMessage());
        } finally {
            BaseContext.removeAll();
        }
    }

    private Long resolveUserId(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object raw = toolContext.getContext().get(CTX_USER_ID);
        if (raw instanceof Long l) {
            return l;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        try {
            return raw == null ? null : Long.valueOf(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Result 解包：code!=1 视为业务失败，抛出 msg 供上层转为 LLM 可读错误 */
    private static <T> T unwrap(Result<T> result) {
        if (result == null || result.getCode() == null || result.getCode() != 1) {
            throw new IllegalStateException(result == null || result.getMsg() == null
                    ? "下游服务响应异常" : result.getMsg());
        }
        return result.getData();
    }

    private static com.sky.dto.cart.ShoppingCartDTO buildCartDTO(Long dishId, Long setmealId, String dishFlavor) {
        com.sky.dto.cart.ShoppingCartDTO dto = new com.sky.dto.cart.ShoppingCartDTO();
        dto.setDishId(dishId);
        dto.setSetmealId(setmealId);
        dto.setDishFlavor(dishFlavor);
        return dto;
    }

    private static Map<String, Object> briefCartItem(ShoppingCart item, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("message", message);
        m.put("name", item.getName());
        if (item.getDishFlavor() != null && !item.getDishFlavor().isBlank()) {
            m.put("flavor", item.getDishFlavor());
        }
        m.put("quantity", item.getNumber());
        return m;
    }

    private static String toJson(Object data) {
        try {
            return MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            return "{\"error\":\"结果序列化失败\"}";
        }
    }

    private static String errorJson(String message) {
        try {
            return MAPPER.writeValueAsString(Map.of("error", message));
        } catch (Exception e) {
            return "{\"error\":\"" + message + "\"}";
        }
    }
}
