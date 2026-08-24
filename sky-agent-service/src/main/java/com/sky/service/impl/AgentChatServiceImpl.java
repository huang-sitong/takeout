package com.sky.service.impl;

import com.sky.exception.AiServiceException;
import com.sky.memory.RedisChatMemory;
import com.sky.service.AgentChatService;
import com.sky.tools.OrderingTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * AI 智能对话服务实现 — 基于 Spring AI ChatClient + Function Calling（Phase B 点餐助手）
 *
 * 工具集 {@link OrderingTools}：浏览菜单 + 管理购物车（权限上限，不含下单）。
 * userId 经 ToolContext 显式传递给工具——SSE 流式下工具执行不在请求线程，
 * 不能依赖 RequestContextHolder。
 *
 * Phase C: Redis ChatMemory 多轮会话记忆（conversationId = userId），
 * 经 MessageChatMemoryAdvisor 自动读写历史，支持"这个换成微辣的"类指代。
 */
@Slf4j
@Service
public class AgentChatServiceImpl implements AgentChatService {

    /**
     * 点餐助手 System Prompt：
     * 1) 划定能力边界——止步于购物车，下单引导用户去结算页；
     * 2) 约束工具使用策略与防幻觉规则；
     * 3) 固定输出风格。
     */
    private static final String ORDERING_SYSTEM_PROMPT = """
            你是「苍穹外卖」小程序的智能点餐助手，用中文为用户提供点餐服务。

            ## 能力边界（严格遵守）
            你只能帮用户完成：查询营业状态、浏览分类/菜品/套餐、购物车的查看与增减清空。
            你没有下单、支付、地址管理的能力。当用户想下单或结账时，
            告知：请在小程序购物车页面核对商品后点击"去结算"完成下单。

            ## 工具使用规则
            1. 菜品/套餐的名称、价格、id 必须以工具返回的数据为准，严禁编造不存在的菜品或价格。
            2. 调用 addToCart/removeFromCart 时，dishId/setmealId 必须来自本会话内 getDishes/getSetmeals
               的返回结果；如果记不确定，必须先重新查询再操作。
            3. 用户问"有什么吃的/推荐一下"时：先 getShopStatus 确认营业中，再 getCategories 查分类，
               根据用户偏好挑相关分类查菜品；已打烊时直接告知，不推荐菜品。
            4. 加购时若用户已明确指定商品和数量（如确认语），无需再次询问；带 flavors 的菜品必须先让用户
               选定口味，再携带 dishFlavor 调用 addToCart。套餐无口味，确认后可直接加购。
            5. 用户说"不要了/去掉一个"时用 removeFromCart；"清空购物车"仅在明确要求时调用 clearCart。
            6. 涉及购物车变动后，可用 getCart 向用户展示当前明细。
            7. 工具返回 {"error":...} 时向用户如实说明原因，并按提示重新获取正确数据重试。
            8. 不要重复调用同一工具获取相同数据；一次回复内只做用户当前意图所需的操作。

            ## 输出风格
            - 简洁友好，适合聊天窗口阅读；价格保留两位小数并以"元"为单位
            - 推荐菜品时给出菜名和价格即可，最多推荐 3~5 个，不要罗列全部
            - 不讨论与点餐无关的话题，礼貌地把话题引回点餐
            """;

    private final ChatClient chatClient;

    private final OrderingTools orderingTools;

    /**
     * 会话记忆 advisor（单例可复用，conversationId 经 request params 传入）。
     * 底层 RedisChatMemory 读写失败均降级不抛异常，不影响对话主流程。
     */
    private final MessageChatMemoryAdvisor memoryAdvisor;

    /**
     * Spring AI 自动装配的是原型作用域的 {@link ChatClient.Builder}（非单例 ChatClient），
     * 需在此手动 build。
     */
    public AgentChatServiceImpl(ChatClient.Builder chatClientBuilder,
                                OrderingTools orderingTools,
                                RedisChatMemory redisChatMemory) {
        this.orderingTools = orderingTools;
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(redisChatMemory).build();
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String chat(String message, Long userId) {
        try {
            String content = chatClient.prompt()
                    .system(ORDERING_SYSTEM_PROMPT)
                    .user(message)
                    .tools(orderingTools)
                    .toolContext(Map.of(OrderingTools.CTX_USER_ID, userId))
                    .advisors(a -> a.advisors(memoryAdvisor)
                            .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                    .call()
                    .content();
            log.info("Agent sync chat done: userId={}, replyLen={}", userId, content == null ? 0 : content.length());
            return content;
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage(), e);
            throw new AiServiceException("AI 助手开小差了，请稍后再试");
        }
    }

    @Override
    public Flux<String> chatStream(String message, Long userId) {
        return chatClient.prompt()
                .system(ORDERING_SYSTEM_PROMPT)
                .user(message)
                .tools(orderingTools)
                .toolContext(Map.of(OrderingTools.CTX_USER_ID, userId))
                .advisors(a -> a.advisors(memoryAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.error("LLM stream failed: {}", e.getMessage(), e);
                    return Flux.just("[AI 服务暂时不可用，请稍后再试]");
                });
    }
}
