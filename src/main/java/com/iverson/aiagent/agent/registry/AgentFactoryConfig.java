package com.iverson.aiagent.agent.registry;

import com.iverson.aiagent.agent.impl.AnalysisAgent;
import com.iverson.aiagent.agent.impl.CopywritingAgent;
import com.iverson.aiagent.agent.impl.SeoAgent;
import com.iverson.aiagent.agent.planner.AgentDefinition;
import com.iverson.aiagent.agent.planner.PlannerAgent;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentFactoryConfig {

    @Autowired
    private ToolCallback[] allTools;

    @Autowired
    private ChatModel dashscopeChatModel;

    @Autowired
    private AgentRegistry registry;

    @PostConstruct
    public void registerAgents() {
        // 注册 AnalysisAgent
        registry.register("analysis",
                () -> {
                    AnalysisAgent agent = new AnalysisAgent(allTools);
                    agent.setChatClient(ChatClient.builder(dashscopeChatModel).build());
                    agent.setMaxSteps(10);
                    return agent;
                },
                new AgentDefinition(
                        "analysis",
                        "商品分析",
                        "分析商品的目标用户、核心卖点、使用场景，输出结构化的分析结果（JSON格式）。",
                        "输入：商品名称和描述（文本）。输出：包含目标用户画像、核心卖点列表、典型使用场景的JSON对象。",
                        List.of("商品描述"),
                        List.of("分析结果JSON")
                )
        );

        // 注册 CopywritingAgent
        registry.register("copywriting",
                () -> {
                    CopywritingAgent agent = new CopywritingAgent(allTools);
                    agent.setChatClient(ChatClient.builder(dashscopeChatModel).build());
                    agent.setMaxSteps(10);
                    return agent;
                },
                new AgentDefinition(
                        "copywriting",
                        "文案生成",
                        "根据商品分析结果生成吸引人的商品标题、详细描述和卖点文案。",
                        "输入：商品分析结果（JSON格式，包含目标用户、卖点、场景）。输出：包含标题、描述、卖点文案的JSON对象。",
                        List.of("分析结果"),
                        List.of("文案JSON")
                )
        );

        // 注册 SeoAgent
        registry.register("seo",
                () -> {
                    SeoAgent agent = new SeoAgent(allTools);
                    agent.setChatClient(ChatClient.builder(dashscopeChatModel).build());
                    agent.setMaxSteps(10);
                    return agent;
                },
                new AgentDefinition(
                        "seo",
                        "SEO优化",
                        "提取关键词，优化标题和描述，提升搜索引擎排名。",
                        "输入：商品名称、分析结果、文案初稿（文本）。输出：包含关键词列表、优化后标题、优化后描述的JSON对象。",
                        List.of("商品名称", "分析结果", "文案"),
                        List.of("SEO优化结果JSON")
                )
        );

        // 注册 PlannerAgent
        registry.register("planner",
                () -> {
                    PlannerAgent agent = new PlannerAgent();
                    agent.setChatClient(ChatClient.builder(dashscopeChatModel).build());
                    agent.setMaxSteps(10);
                    return agent;
                },
                new AgentDefinition(
                        "planner",
                        "任务规划",
                        "根据用户输入生成多智能体协作的任务计划。",
                        "输入：用户输入的商品信息。输出：包含任务列表和依赖关系的JSON对象。",
                        List.of("用户输入"),
                        List.of("任务计划JSON")
                )
        );
    }
}