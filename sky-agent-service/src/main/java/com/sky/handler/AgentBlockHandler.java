package com.sky.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.sky.dto.agent.ChatRequest;
import com.sky.result.Result;
import reactor.core.publisher.Flux;

/**
 * sky-agent-service Sentinel 阻塞处理类。
 *
 * 方法签名必须与原 Controller 方法逐参数匹配并追加 BlockException 参数，
 * 返回类型与原方法一致，且方法必须是 static——否则 BlockException 会被当 500 上抛。
 * 规则由 Nacos 数据源 (sky-agent-service-flow-rules.json) 持久化下发。
 *
 * LLM 调用成本高且响应慢，两个接口均按资源级限流保护。
 */
public class AgentBlockHandler {

    private static String failMsg(BlockException ex) {
        if (ex instanceof DegradeException) {
            return "AI 助手暂时不可用，请稍后再试";
        }
        return "当前咨询人数较多，请稍后再试";
    }

    /** 同步对话限流降级 */
    public static Result<String> chatBlock(ChatRequest request, Long userId, BlockException ex) {
        return Result.error(failMsg(ex));
    }

    /** 流式对话限流降级：SSE 通道返回一条提示文本后关闭 */
    public static Flux<String> streamBlock(ChatRequest request, Long userId, BlockException ex) {
        return Flux.just("[" + failMsg(ex) + "]");
    }
}
