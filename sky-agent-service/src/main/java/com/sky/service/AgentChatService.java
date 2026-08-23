package com.sky.service;

import reactor.core.publisher.Flux;

/**
 * AI 智能对话服务
 */
public interface AgentChatService {

    /**
     * 同步对话：阻塞直至 LLM 返回完整回复（支持 Function Calling 工具调用）
     *
     * @param message 用户消息
     * @param userId  当前登录用户id（Gateway 注入的 X-User-Id，经 ToolContext 传给工具）
     * @return 助手完整回复文本
     */
    String chat(String message, Long userId);

    /**
     * 流式对话：以 SSE 增量片段形式逐段返回（支持 Function Calling 工具调用）
     *
     * @param message 用户消息
     * @param userId  当前登录用户id
     * @return 文本增量流
     */
    Flux<String> chatStream(String message, Long userId);
}
