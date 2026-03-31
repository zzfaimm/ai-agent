package com.iverson.aiagent.agent.multi.impl;


import com.iverson.aiagent.agent.multi.core.ToolCallAgent;
import org.springframework.ai.tool.ToolCallback;

/**
 * 商品分析Agent
 */

public class AnalysisAgent extends ToolCallAgent {

    public AnalysisAgent(ToolCallback[] tools) {

        super(tools);

        this.setName("analysis");

        this.setSystemPrompt("""
        你是一个电商商品分析专家。根据用户输入的商品信息，输出一个严格的 JSON 对象，不要有任何额外解释。
        
        格式如下：
        {
          "targetAudience": "25-45岁职场人士与运动爱好者",
          "sellingPoints": ["卖点1", "卖点2", "卖点3"],
          "scenarios": ["场景1", "场景2", "场景3"]
        }
        
        注意：
        - 如果需要更多商品信息，可以使用 WebSearchTool 进行网络搜索
        - 如果需要参考类似商品的分析，可以使用 WebScrapingTool 抓取相关网页
        - 不要反问用户，直接基于输入内容和获取的信息生成结果
        - 你必须在一轮对话中同时输出这个 JSON 并调用 doTerminate 工具
        - doTerminate 的 finalResult 参数就是上面的 JSON 字符串
        - 确保输出的 JSON 格式正确且完整，以便后续智能体能够正确解析
        """);

        this.setNextStepPrompt("""
        根据当前任务进度，决定下一步操作。
        如果需要外部数据，选择合适的工具。
        **关键**：当你获得分析结果后，必须在本轮直接输出JSON并调用doTerminate，不要等待下一轮。
        如果你已经完成了最终结果，必须在输出结果的同时调用 doTerminate 工具，不要分两轮。
        每次使用工具后，说明结果并规划下一步。
        若想在任何时候停止，调用 doTerminate。
        """);

//        this.setChatClient(ChatClient.builder(dashscopeChatModel).build());

        // 🔥 注册
//        registry.register("analysis", "商品分析", this);
//        registry.register("analysis", "商品分析", () -> new AnalysisAgent(registry, tools, dashscopeChatModel));
    }
}
