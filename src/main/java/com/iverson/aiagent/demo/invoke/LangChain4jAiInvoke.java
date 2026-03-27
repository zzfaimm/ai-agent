package com.iverson.aiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;

/**
 * @ClassName: LangChain4jAiInvoke
 * @Description: TODO(描述这个类的作用)
 * @Author: zhuze
 * @Date: 3/12/2026 5:41 PM
 * @Version: 1.0
 */
public class LangChain4jAiInvoke {
    public static void main(String[] args) {
        QwenChatModel qwenChatModel = QwenChatModel.builder()
                .apiKey(TestApiKey.apiKey)
                .modelName("qwen-plus")
                .build();
        String chat = qwenChatModel.chat("你好我正在测试langchain4j调用大模型");
        System.out.println(chat);
    }
}
