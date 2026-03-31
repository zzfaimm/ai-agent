package com.iverson.aiagent.agent.multi.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iverson.aiagent.agent.multi.core.BaseAgent;
import com.iverson.aiagent.agent.multi.model.AgentResult;
import com.iverson.aiagent.agent.multi.model.SharedState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * 规划智能体
 * 用于生成多智能体协作的任务计划
 */
@Slf4j
@Component
public class PlannerAgent extends BaseAgent {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlannerAgent() {
        this.setName("planner");
        this.setSystemPrompt("""
        你是一个多智能体协作规划专家。你的任务是根据用户的输入，生成一个合理的多智能体协作任务计划。
        
        可用智能体列表（必须使用以下名称）：
        1. analysis - 商品分析智能体：分析商品的目标用户、核心卖点、使用场景
        2. copywriting - 文案生成智能体：根据分析结果生成吸引人的商品标题和描述
        3. critic - 文案评估智能体：评估文案质量，提供反馈和优化建议
        4. seo - SEO优化智能体：提取关键词，优化标题和描述，提升搜索引擎排名
        
        请根据以下步骤生成计划：
        1. 分析用户输入，确定任务类型和目标
        2. 从可用智能体列表中选择需要的智能体
        3. 规划智能体之间的协作顺序和依赖关系
        
        输出格式为JSON：
        {
          "tasks": [
            {
              "name": "智能体名称（必须使用英文名称：analysis/copywriting/seo）",
              "dependsOn": ["依赖的智能体名称列表"]
            }
          ]
        }
        
        注意：
        - 智能体名称必须使用英文：analysis、copywriting、critic、seo
        - 依赖关系应该合理，确保任务按照正确的顺序执行
        - 不要包含不必要的智能体
        - 通常的执行顺序是：analysis -> copywriting -> critic -> seo
        - critic智能体评估文案质量，如需要重写会自动调用copywriting智能体
        """);
    }

    /**
     * 生成任务计划
     * @param input 用户输入
     * @return 任务计划
     */
    public Plan plan(String input) {
        log.info("{}: 开始生成任务计划，用户输入: {}", getName(), input);
        
        // 构建提示词
        String prompt = getSystemPrompt() + "\n\n用户输入：\n" + input;
        
        // 调用LLM生成计划
        ChatClient chatClient = getChatClient();
        if (chatClient == null) {
            log.error("{}: 聊天客户端未初始化", getName());
            return new Plan(new ArrayList<>());
        }
        
        String response = chatClient.prompt()
                .messages(new UserMessage(prompt))
                .call()
                .content();
        
        log.info("{}: 生成的计划: {}", getName(), response);
        
        try {
            // 解析计划
            Plan plan = objectMapper.readValue(response, Plan.class);
            log.info("{}: 计划解析成功，任务数量: {}", getName(), plan.getTasks().size());
            return plan;
        } catch (Exception e) {
            log.error("{}: 解析计划失败", getName(), e);
            return new Plan(new ArrayList<>());
        }
    }

    @Override
    protected AgentResult doRun(SharedState sharedState) {
        log.info("{}: 开始执行规划任务", getName());
        
        // 获取用户输入
        String input = sharedState.getUserInput();
        if (input == null) {
            input = sharedState.getCurrentContext();
        }
        
        if (input == null) {
            log.warn("{}: 没有找到用户输入", getName());
            AgentResult result = new AgentResult();
            result.setSuccess(false);
            result.setActionType(AgentResult.ActionType.RETRY);
            return result;
        }
        
        // 生成计划
        Plan plan = plan(input);
        
        // 将计划转换为字符串
        String planJson;
        try {
            planJson = objectMapper.writeValueAsString(plan);
        } catch (Exception e) {
            log.error("{}: 计划转换失败", getName(), e);
            AgentResult result = new AgentResult();
            result.setSuccess(false);
            result.setActionType(AgentResult.ActionType.RETRY);
            return result;
        }
        
        // 存储计划
        sharedState.put(this.getName(), planJson, this.getName());
        sharedState.putStructuredData(this.getName(), plan, this.getName());
        
        // 创建AgentResult
        AgentResult result = new AgentResult();
        result.setSuccess(true);
        result.setFinalAnswer(planJson);
        result.setActionType(AgentResult.ActionType.FINISH);
        
        return result;
    }

    @Override
    public String step() {
        // 此方法不会被调用，因为我们重写了doRun方法
        return "PlannerAgent不需要执行step方法";
    }
}
