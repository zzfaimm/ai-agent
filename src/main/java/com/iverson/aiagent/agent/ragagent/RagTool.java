package com.iverson.aiagent.agent.ragagent;

import com.iverson.aiagent.rag.LoveAppRagCustomAdvisorFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RAG工具，使用现有的RAG功能
 */
@Component
public class RagTool implements Tool {
    
    private final ChatClient chatClient;
    
    @Autowired
    public RagTool(ChatModel dashscopeChatModel, VectorStore vectorStore) {
        // 创建包含RAG增强的ChatClient
        Advisor ragAdvisor = LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(vectorStore, "active");
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(ragAdvisor)
                .build();
    }
    
    @Override
    public String getName() {
        return "rag_tool";
    }
    
    @Override
    public String getDescription() {
        return "用于回答恋爱相关问题的工具，会从知识库中检索相关信息";
    }
    
    @Override
    public String execute(String input) {
        return chatClient.prompt()
                .user(input)
                .call()
                .content();
    }
}
