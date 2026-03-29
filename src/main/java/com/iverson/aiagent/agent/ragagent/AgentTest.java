//package com.iverson.aiagent.agent.ragagent;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
///**
// * Agent测试类
// */
//@Component
//public class AgentTest implements CommandLineRunner {
//
//    @Autowired
//    private Agent agent;
//
//    @Override
//    public void run(String... args) throws Exception {
//        System.out.println("=== Agentic RAG 测试 ===");
//
//        // 测试问题
//        String[] testQueries = {
//            "如何处理异地恋？",
//            "女朋友生气了怎么办？",
//            "如何表白？"
//        };
//
//        for (String query : testQueries) {
//            System.out.println("\n用户: " + query);
//            String response = agent.process(query);
//            System.out.println("代理: " + response);
//        }
//
//        System.out.println("\n=== 测试完成 ===");
//    }
//}
