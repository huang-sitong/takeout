package com.sky.service;

import reactor.core.publisher.Flux;

/**
 * AI 智能对话服务
 */
public interface AgentChatService {

    /**
     * 同步对话：阻塞直至 LLM 返回完整回复
     *
     * @param message 用户消息
     * @return 助手完整回复文本
     */
    String chat(String message);

    /**
     * 流式对话：以 SSE 增量片段形式逐段返回
     *
     * @param message 用户消息
     * @return 文本增量流
     */
    Flux<String> chatStream(String message);
}
