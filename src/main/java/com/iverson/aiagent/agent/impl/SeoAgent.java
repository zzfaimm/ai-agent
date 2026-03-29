package com.iverson.aiagent.agent.impl;

import com.iverson.aiagent.agent.base.ToolCallAgent;
import org.springframework.ai.tool.ToolCallback;


public class SeoAgent extends ToolCallAgent {

    public SeoAgent(ToolCallback[] tools) {

        super(tools);

        this.setName("seo");

        this.setSystemPrompt("""
        你是一个 SEO 专家。你的输入是商品文案（JSON 格式），包含标题、描述、卖点文案。
        
        请进行以下优化，输出 JSON 格式：
        1. 提取 5-8 个核心关键词（基于商品卖点和用户搜索习惯）
        2. 优化标题（在原标题基础上融入 1-2 个关键词，长度 25 字以内）
        3. 优化描述（在原文案基础上自然嵌入关键词，保持可读性，180 字以内）
        
        输出格式示例：
        {
          "keywords": ["关键词1", "关键词2", ...],
          "optimizedTitle": "...",
          "optimizedDescription": "..."
        }
        
        注意：
        - 不要反问用户，直接基于输入内容优化。
        - 完成优化后，必须调用 doTerminate 工具结束任务。
        - 确保输出的 JSON 格式正确且完整。
        - 如果你收到的输入不是完整的 JSON 格式，请尝试从输入中提取商品文案信息，然后进行 SEO 优化。
        - 你可以使用 searchWeb 工具获取更多相关信息，但优先使用前一个智能体提供的信息。
        """);

        this.setNextStepPrompt("""
        根据当前任务进度，决定下一步操作。
        如果需要外部数据，选择合适的工具。
        **如果已经可以输出最终结果，请在本轮中同时输出 JSON 并调用 doTerminate 工具。**
        每次使用工具后，说明结果并规划下一步。
        若想在任何时候停止，调用 doTerminate。
        """);

//        this.setChatClient(ChatClient.builder(dashscopeChatModel).build());

//        registry.register("seo", "SEO优化", this);
//        registry.register("seo", "SEO优化", () -> new SeoAgent(registry, tools, dashscopeChatModel));

    }
}
