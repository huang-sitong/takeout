package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 对话请求体
 */
@Data
public class ChatRequest implements Serializable {

    private String message;
}
