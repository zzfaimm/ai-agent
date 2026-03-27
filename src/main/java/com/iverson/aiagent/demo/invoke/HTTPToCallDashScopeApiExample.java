package com.iverson.aiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * 使用 Hutool 调用阿里云 DashScope API 的示例,http的方式调用API
 */
public class HTTPToCallDashScopeApiExample {

    // 从环境变量获取 API Key，你也可以直接赋值（不推荐硬编码）
    private static final String API_KEY = TestApiKey.apiKey;  // 直接使用类中的静态变量


    // API 请求地址
    private static final String URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    public static void main(String[] args) {
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", "qwen-plus");

        JSONObject input = new JSONObject();
        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", "You are a helpful assistant.");

        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", "你好，我正在开发大模型智能体？");

        input.set("messages", new JSONObject[]{systemMsg, userMsg});
        requestBody.set("input", input);

        JSONObject parameters = new JSONObject();
        parameters.set("result_format", "message");
        requestBody.set("parameters", parameters);

        // 发送 POST 请求
        try (HttpResponse response = HttpRequest.post(URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout(20000) // 设置超时时间（毫秒）
                .execute()) {

            // 检查响应状态码
            if (response.isOk()) {
                // 解析响应 JSON
                JSONObject jsonResponse = JSONUtil.parseObj(response.body());
                System.out.println("调用成功，响应内容：");
                System.out.println(jsonResponse.toStringPretty());
            } else {
                System.err.println("HTTP 错误码: " + response.getStatus());
                System.err.println("错误响应: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("请求异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}