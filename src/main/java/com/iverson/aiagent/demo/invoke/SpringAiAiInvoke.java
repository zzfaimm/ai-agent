//package com.iverson.aiagent.demo.invoke;
//
//import jakarta.annotation.Resource;
//import org.springframework.ai.chat.messages.AssistantMessage;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.ai.chat.prompt.Prompt;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
///**
// * @ClassName: SpringAiAiInvoke
// * @Description: spring AI调用大模型
// * @Author: zhuze
// * @Date: 3/12/2026 5:05 PM
// * @Version: 1.0
// */
//@Component
//public class SpringAiAiInvoke implements CommandLineRunner {
//
//    @Resource
//    @Qualifier("dashscopeChatModel")
//    private ChatModel dashScopeChatModel;
//    @Override
//    public void run(String... args) throws Exception {
//        AssistantMessage message = dashScopeChatModel.call(new Prompt("你好我正在使用Spring AI 对大模型进行调用"))
//                .getResult()
//                .getOutput();
//        System.out.println("Spring AI 调用测试");
//        System.out.println(message.getText());
//    }
//}
