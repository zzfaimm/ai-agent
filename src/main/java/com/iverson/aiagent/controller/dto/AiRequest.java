package com.iverson.aiagent.controller.dto;

import lombok.Data;

/**
 * 请求参数
 */
@Data
public class AiRequest {

    /**
     * 商品信息 / 用户输入
     */
    private String input;
}