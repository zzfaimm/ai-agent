package com.iverson.aiagent.agent.impl;


import com.iverson.aiagent.agent.base.ToolCallAgent;
import org.springframework.ai.tool.ToolCallback;

/**
 * 商品分析Agent
 */

public class AnalysisAgent extends ToolCallAgent {

    public AnalysisAgent(ToolCallback[] tools) {

        super(tools);

        this.setName("analysis");

        this.setSystemPrompt("""
        你是一个电商商品分析专家。你的输入是用户提供的商品名称和描述。
        
        请严格按照以下步骤进行分析，并输出 JSON 格式的结果：
        1. 确定目标用户群体（年龄、职业、使用习惯等）
        2. 提炼核心卖点（3-5 个关键优势）
        3. 列举典型使用场景（3 个以上）
        
        输出格式示例：
        {
          "targetAudience": "...",
          "sellingPoints": ["...", "...", "..."],
          "scenarios": ["...", "...", "..."]
        }
        
        注意：
        - 不要反问用户，直接基于输入内容生成结果。
        - 完成分析后，必须调用 doTerminate 工具结束任务。
        - 确保输出的 JSON 格式正确且完整，以便后续智能体能够正确解析。
        """);

        this.setNextStepPrompt("""
        根据当前任务进度，决定下一步操作。
        如果需要外部数据，选择合适的工具。
        **如果已经可以输出最终结果，请在本轮中同时输出 JSON 并调用 doTerminate 工具。**
        每次使用工具后，说明结果并规划下一步。
        若想在任何时候停止，调用 doTerminate。
        """);

//        this.setChatClient(ChatClient.builder(dashscopeChatModel).build());

        // 🔥 注册
//        registry.register("analysis", "商品分析", this);
//        registry.register("analysis", "商品分析", () -> new AnalysisAgent(registry, tools, dashscopeChatModel));
    }
}
