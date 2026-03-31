package com.iverson.aiagent.agent.multi.impl;

import com.iverson.aiagent.agent.multi.core.ToolCallAgent;
import org.springframework.ai.tool.ToolCallback;


public class CopywritingAgent extends ToolCallAgent {

    public CopywritingAgent(ToolCallback[] tools) {

        super(tools);

        this.setName("copywriting");

        this.setSystemPrompt("""
        你是电商文案专家。你的输入是商品分析JSON（包含targetAudience, sellingPoints, scenarios）。
        你必须基于输入生成以下JSON，并在**同一轮响应中**同时输出JSON并调用doTerminate。
        
        输出格式：
        {
          "title": "20字以内的商品标题",
          "description": "150字左右的商品描述",
          "sellingPoints": ["卖点1", "卖点2", "卖点3"]
        }
        
        注意：
        - 不要输出任何额外解释，只输出上述JSON，然后立即调用doTerminate
        - 如果需要了解市场趋势和竞品情况，可以使用 WebSearchTool 进行网络搜索
        - 如果需要参考优秀的同类商品文案，可以使用 WebScrapingTool 抓取相关网页
        - 确保输出的 JSON 格式正确且完整，以便后续智能体能够正确解析
        - **输入解析策略**：如果你收到的输入不是完整的 JSON 格式（例如是总结文本），请使用以下策略：
          1. 首先尝试从输入中提取关键信息：目标用户群体、核心卖点、使用场景
          2. 如果无法直接提取，可以调用 LLM 进行信息提取
          3. 基于提取的信息生成文案
        """);

        this.setNextStepPrompt("""
        根据当前任务进度，决定下一步操作。
        **关键**：当你获得分析结果后，必须在本轮直接输出文案JSON并调用doTerminate，不要等待下一轮。
        如果你已经完成了最终结果，必须在输出结果的同时调用 doTerminate 工具，不要分两轮。
        如果需要外部数据，选择合适的工具。
        每次使用工具后，说明结果并规划下一步。
        若想在任何时候停止，调用doTerminate。
        """);

//        this.setChatClient(ChatClient.builder(dashscopeChatModel).build());

//        registry.register("copywriting", "文案生成", this);
//        registry.register("copywriting", "文案生成", () -> new CopywritingAgent(registry, tools, dashscopeChatModel));

    }
}
