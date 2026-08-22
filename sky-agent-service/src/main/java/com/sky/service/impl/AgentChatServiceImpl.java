package com.sky.service.impl;

import com.sky.exception.AiServiceException;
import com.sky.service.AgentChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 智能对话服务实现 — 基于 Spring AI ChatClient
 *
 * Phase A 为无状态单轮对话（无历史记忆）；Phase C 引入 Redis ChatMemory 多轮会话。
 */
@Slf4j
@Service
public class AgentChatServiceImpl implements AgentChatService {

    private final ChatClient chatClient;

    /**
     * Spring AI 自动装配的是原型作用域的 {@link ChatClient.Builder}（非单例 ChatClient），
     * 需在此手动 build。
     */
    public AgentChatServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String chat(String message) {
        try {
            String content = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();
            log.info("Agent sync chat done: replyLen={}", content == null ? 0 : content.length());
            return content;
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage(), e);
            throw new AiServiceException("AI 助手开小差了，请稍后再试");
        }
    }

    @Override
    public Flux<String> chatStream(String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content()
                .onErrorResume(e -> {
                    log.error("LLM stream failed: {}", e.getMessage(), e);
                    return Flux.just("[AI 服务暂时不可用，请稍后再试]");
                });
    }
}
