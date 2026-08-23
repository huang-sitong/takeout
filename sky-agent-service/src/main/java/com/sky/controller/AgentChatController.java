package com.sky.controller;

import com.sky.dto.agent.ChatRequest;
import com.sky.result.Result;
import com.sky.service.AgentChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * AI 智能助手对话接口
 *
 * 路径挂在 /user/agent/** 下 → Gateway JwtAuthGlobalFilter 按 user 身份校验 JWT，
 * 校验通过后注入 X-User-Id / X-User-Role（Phase B 工具调用时经 FeignInterceptor 透传下游）。
 *
 * SSE 流式接口：响应为 text/event-stream，每个 data: 为一段增量文本；
 * 前端用 fetch + ReadableStream 或 EventSource 解析拼接。
 */
@Slf4j
@RestController("agentChatController") // 显式命名，避免与其他服务的 Controller Bean 冲突
@RequestMapping("/user/agent")
@RequiredArgsConstructor
public class AgentChatController {

    private final AgentChatService agentChatService;

    /**
     * 同步对话 — POST /user/agent/chat
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request,
                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("Agent sync chat: userId={}, msgLen={}", userId,
                request.getMessage() == null ? 0 : request.getMessage().length());
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return Result.error("消息不能为空");
        }
        return Result.success(agentChatService.chat(request.getMessage(), userId));
    }

    /**
     * 流式对话 (SSE) — POST /user/agent/chat/stream
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request,
                                   @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("Agent stream chat: userId={}, msgLen={}", userId,
                request.getMessage() == null ? 0 : request.getMessage().length());
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return Flux.just("[消息不能为空]");
        }
        return agentChatService.chatStream(request.getMessage(), userId);
    }
}
