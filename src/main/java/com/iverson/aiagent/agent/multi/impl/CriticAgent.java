package com.iverson.aiagent.agent.multi.impl;

import com.iverson.aiagent.agent.multi.core.BaseAgent;
import com.iverson.aiagent.agent.multi.model.AgentResult;
import com.iverson.aiagent.agent.multi.model.SharedState;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Map;

/**
 * 评估智能体
 * 用于评估其他智能体的输出质量，提供反馈和优化建议
 */
@Slf4j
public class CriticAgent extends BaseAgent {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CriticAgent() {
        this.setName("critic");
        this.setSystemPrompt("""
        你是一个专业的文案评估专家。你的任务是评估电商商品文案的质量，并给出客观的评分和建议。
        
        评估标准：
        1. 吸引力：文案是否能够吸引目标用户的注意力
        2. 逻辑性：文案是否逻辑清晰，信息组织合理
        3. 购买欲：文案是否能够激发用户的购买欲望
        4. 准确性：文案是否准确反映产品特点
        5. 简洁性：文案是否简洁明了，没有冗余信息
        
        请根据以上标准，对提供的文案进行评估，返回JSON格式的评估结果：
        {
          "score": 1-10,  // 整体评分
          "needRewrite": true/false,  // 是否需要重写
          "comments": "评估意见和建议"
        }
        
        注意：
        - 评分要客观公正，基于文案的实际质量
        - 当评分低于7分时，建议重写
        - 提供具体的改进建议
        """);
    }

    @Override
    protected AgentResult doRun(SharedState sharedState) {
        log.info("{}: 开始评估文案质量", getName());
        
        // 获取需要评估的文案
        String content = (String) sharedState.get("copywriting");
        if (content == null) {
            content = (String) sharedState.get("seo");
        }
        
        if (content == null) {
            log.warn("{}: 没有找到需要评估的文案", getName());
            AgentResult result = new AgentResult();
            result.setSuccess(true);
            result.setActionType(AgentResult.ActionType.CONTINUE);
            return result;
        }
        
        log.info("{}: 评估文案内容: {}", getName(), content);
        
        // 检查文案内容是否为总结文本而非实际文案
        if (content.contains("最终结论：") || content.contains("执行过程简述：") || content.contains("任务完成状态：")) {
            log.warn("{}: 检测到文案为总结文本，跳过评估", getName());
            AgentResult result = new AgentResult();
            result.setSuccess(true);
            result.setActionType(AgentResult.ActionType.CONTINUE);
            return result;
        }
        
        // 构建评估提示词
        String prompt = getSystemPrompt() + "\n\n需要评估的文案：\n" + content;
        
        // 调用LLM进行评估
        ChatClient chatClient = getChatClient();
        if (chatClient == null) {
            log.error("{}: 聊天客户端未初始化", getName());
            AgentResult result = new AgentResult();
            result.setSuccess(false);
            result.setActionType(AgentResult.ActionType.RETRY);
            return result;
        }
        
        String response = chatClient.prompt()
                .messages(new UserMessage(prompt))
                .call()
                .content();
        
        log.info("{}: 评估结果: {}", getName(), response);
        
        try {
            // 解析评估结果
            Map<String, Object> resultMap = objectMapper.readValue(response, Map.class);
            int score = ((Number) resultMap.get("score")).intValue();
            boolean needRewrite = (Boolean) resultMap.get("needRewrite");
            String comments = (String) resultMap.get("comments");
            
            log.info("{}: 评估得分: {}, 是否需要重写: {}", getName(), score, needRewrite);
            
            // 创建AgentResult
            AgentResult result = new AgentResult();
            result.setSuccess(true);
            result.setData(resultMap);
            
            // 根据评估结果决定下一步动作
            if (score < 7 || needRewrite) {
                // 评分低于7分或需要重写，调用copywriting智能体重写
                result.setActionType(AgentResult.ActionType.CALL_AGENT);
                result.setTargetAgent("copywriting");
                result.setNeedsRewrite(true);
                log.info("{}: 评分低于7分，需要重写文案", getName());
            } else {
                // 评分合格，继续执行下一步
                result.setActionType(AgentResult.ActionType.CONTINUE);
                result.setNeedsRewrite(false);
                log.info("{}: 评分合格，继续执行下一步", getName());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("{}: 解析评估结果失败", getName(), e);
            AgentResult result = new AgentResult();
            result.setSuccess(true);
            result.setActionType(AgentResult.ActionType.CONTINUE);
            return result;
        }
    }

    @Override
    public String step() {
        // 此方法不会被调用，因为我们重写了doRun方法
        return "CriticAgent不需要执行step方法";
    }
}
