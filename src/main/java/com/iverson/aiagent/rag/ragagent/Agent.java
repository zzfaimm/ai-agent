package com.iverson.aiagent.rag.ragagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent核心类，实现ReAct模式
 */
@Component
public class Agent {
    
    private final ChatClient chatClient;
    private final Map<String, Tool> tools;
    private final MemorySystem memory;
    
    @Autowired
    public Agent(ChatModel dashscopeChatModel, List<Tool> tools) {
        this.chatClient = ChatClient.builder(dashscopeChatModel).build();
        this.tools = tools.stream()
                .collect(Collectors.toMap(Tool::getName, tool -> tool));
        this.memory = new MemorySystem();
    }
    
    /**
     * 处理用户查询
     */
    public String process(String query) {
        // 添加用户查询到记忆
        memory.add("用户: " + query);
        
        // 构建工具描述
        String toolDescriptions = tools.values().stream()
                .map(tool -> "工具名称: " + tool.getName() + ", 描述: " + tool.getDescription())
                .collect(Collectors.joining("\n"));
        
        // 构建推理提示
        String prompt = """
        你是一个智能代理，需要根据用户的问题决定使用什么工具来回答。
        
        工具列表:
        """ + toolDescriptions + """
        
        对话历史:
        """ + memory.getSummary() + """
        
        请按照以下格式输出:
        思考: [你的思考过程]
        行动: [工具名称] [工具输入]
        观察: [工具执行结果]
        回答: [最终回答]
        """;
        
        // 获取模型推理结果
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        
        // 解析并执行工具调用
        if (response.contains("行动: ")) {
            String actionLine = response.split("行动: ")[1].split("\n")[0].trim();
            String[] actionParts = actionLine.split(" ", 2);
            if (actionParts.length == 2) {
                String toolName = actionParts[0];
                String toolInput = actionParts[1];
                
                // 执行工具
                if (tools.containsKey(toolName)) {
                    Tool tool = tools.get(toolName);
                    String toolResult = tool.execute(toolInput);
                    
                    // 添加工具执行结果到记忆
                    memory.add("工具结果: " + toolResult);
                    
                    // 生成最终回答
                    String finalPrompt = """
                    根据对话历史和工具执行结果，生成最终回答:
                    
                    对话历史:
                    """ + memory.getSummary() + """
                    
                    请生成简洁、准确的最终回答:
                    """;
                    
                    String finalAnswer = chatClient.prompt()
                            .user(finalPrompt)
                            .call()
                            .content();
                    
                    // 添加最终回答到记忆
                    memory.add("代理: " + finalAnswer);
                    
                    return finalAnswer;
                }
            }
        }
        
        // 如果没有工具调用，直接回答
        memory.add("代理: " + response);
        return response;
    }
    
    /**
     * 清空记忆
     */
    public void clearMemory() {
        memory.clear();
    }
}
