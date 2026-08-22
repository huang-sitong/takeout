package com.sky.exception;

/**
 * AI 服务业务异常 — LLM 调用失败/超时等场景
 * 由 sky-common 的 GlobalExceptionHandler 统一捕获返回 Result
 */
public class AiServiceException extends BaseException {

    public AiServiceException() {
    }

    public AiServiceException(String msg) {
        super(msg);
    }
}
