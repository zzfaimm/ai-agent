package com.iverson.aiagent.agent.multi.impl;

import com.iverson.aiagent.agent.multi.core.ToolCallAgent;
import org.springframework.ai.tool.ToolCallback;


public class SeoAgent extends ToolCallAgent {

    public SeoAgent(ToolCallback[] tools) {

        super(tools);

        this.setName("seo");

        this.setSystemPrompt("""
        你是SEO专家。你的输入是商品文案JSON（包含title, description, sellingPoints）。
        你必须输出以下JSON，并在同一轮调用doTerminate：
        {
          "keywords": ["关键词1", ...],
          "optimizedTitle": "优化后的标题（25字以内）",
          "optimizedDescription": "优化后的描述（180字以内）"
        }
        
        注意：
        - 不要输出任何额外解释，只输出上述JSON，然后立即调用doTerminate
        - 如果需要了解用户搜索习惯和热门关键词，可以使用 WebSearchTool 进行网络搜索
        - 如果需要参考同类商品的SEO优化策略，可以使用 WebScrapingTool 抓取相关网页
        - 确保输出的 JSON 格式正确且完整
        - 如果你收到的输入不是完整的 JSON 格式，请尝试从输入中提取商品文案信息，然后进行 SEO 优化
        """);

        this.setNextStepPrompt("""
        根据当前任务进度，决定下一步操作。
        **关键**：当你获得文案结果后，必须在本轮直接输出SEO优化JSON并调用doTerminate，不要等待下一轮。
        如果需要外部数据，选择合适的工具。
        每次使用工具后，说明结果并规划下一步。
        若想在任何时候停止，调用doTerminate。
        """);

//        this.setChatClient(ChatClient.builder(dashscopeChatModel).build());

//        registry.register("seo", "SEO优化", this);
//        registry.register("seo", "SEO优化", () -> new SeoAgent(registry, tools, dashscopeChatModel));

    }
}
