package com.iverson.aiagent.agent.impl;

import com.iverson.aiagent.agent.base.ToolCallAgent;
import org.springframework.ai.tool.ToolCallback;


public class CopywritingAgent extends ToolCallAgent {

    public CopywritingAgent(ToolCallback[] tools) {

        super(tools);

        this.setName("copywriting");

        this.setSystemPrompt("""
        你是一个电商文案专家。你的输入是商品分析结果（JSON 格式），包含目标用户、卖点、使用场景。
        
        请基于输入生成以下三部分文案，输出 JSON 格式：
        1. 商品标题（20 字以内，吸引眼球）
        2. 商品描述（150 字左右，突出核心价值）
        3. 卖点文案（3 条，每条 30 字以内，适合详情页或短视频脚本）
        
        输出格式示例：
        {
          "title": "...",
          "description": "...",
          "sellingPoints": ["...", "...", "..."]
        }
        
        注意：
        - 不要反问用户，直接根据输入生成文案。
        - 完成文案生成后，必须调用 doTerminate 工具结束任务。
        - 确保输出的 JSON 格式正确且完整，以便后续智能体能够正确解析。
        - 如果你收到的输入不是完整的 JSON 格式，请尝试从输入中提取商品分析信息，然后生成文案。
        """);

        this.setNextStepPrompt("""
        根据当前任务进度，决定下一步操作。
        如果需要外部数据，选择合适的工具。
        **如果已经可以输出最终结果，请在本轮中同时输出 JSON 并调用 doTerminate 工具。**
        每次使用工具后，说明结果并规划下一步。
        若想在任何时候停止，调用 doTerminate。
        """);

//        this.setChatClient(ChatClient.builder(dashscopeChatModel).build());

//        registry.register("copywriting", "文案生成", this);
//        registry.register("copywriting", "文案生成", () -> new CopywritingAgent(registry, tools, dashscopeChatModel));

    }
}
