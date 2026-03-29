package com.iverson.aiagent.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 返回结果
 */
@Data
@AllArgsConstructor
public class AiResponse {

    /**
     * AI生成结果
     */
    private String result;
}