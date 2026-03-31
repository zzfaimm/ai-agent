package com.iverson.aiagent.agent.multi.core;

import com.iverson.aiagent.agent.multi.model.AgentResult;
import com.iverson.aiagent.agent.multi.model.AgentState;
import com.iverson.aiagent.agent.multi.model.SharedState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: ReActAgent
 * @Description: ReAct风格的代理类，实现了思考-行动-观察的循环
 * @Author: zhuze
 * @Date: 3/26/2026 6:18 PM
 * @Version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    @Override
    protected AgentResult doRun(SharedState sharedState) {
        log.info("{}: 开始执行ReAct循环", getName());
        
        if (this.state != AgentState.IDLE) {
            log.warn("{}: 无法从状态运行代理: {}", getName(), this.state);
            AgentResult result = new AgentResult();
            result.setSuccess(false);
            result.setActionType(AgentResult.ActionType.RETRY);
            return result;
        }

        this.state = AgentState.RUNNING;
        log.info("{}: 状态变更为: {}", getName(), state);
        this.sharedState = sharedState;
        log.info("{}: 已设置共享状态", getName());

        // 优先使用结构化输入
        Object structuredInput = sharedState.getCurrentStructuredInput();
        String input = structuredInput != null ? structuredInput.toString() : sharedState.getUserInput();
        log.info("{}: 使用输入: {}", getName(), input);
        
        if (input == null) {
            input = sharedState.getUserInput();
            log.info("{}: 使用用户输入: {}", getName(), input);
        }

        getMessageList().add(new org.springframework.ai.chat.messages.UserMessage(input));
        log.info("{}: 已添加输入到消息上下文", getName());

        String lastStepResult = null;

        try {

            // =====================
            // 1️⃣ 执行阶段
            // =====================
            for (int i = 0; i < getMaxSteps() && state != AgentState.FINISHED; i++) {

                setCurrentStep(i + 1);
                log.info("{}: 执行步骤 {}/{}", getName(), getCurrentStep(), getMaxSteps());

                // 思考
                boolean needAct = think();
                log.info("{}: 思考完成，是否需要行动: {}", getName(), needAct);

                // 行动
                if (needAct) {
                    String stepResult = act();
                    log.info("{}: 行动完成，结果: {}", getName(), stepResult);
                    lastStepResult = stepResult;
                } else {
                    lastStepResult = "思考完成 - 无需行动";
                    log.info("{}: 无需行动", getName());
                }

                // 🔥 记录执行轨迹
                if("思考完成 - 无需行动".equalsIgnoreCase(lastStepResult)) {
                    sharedState.addTrace(this.getName(),
                            "Step " + getCurrentStep() + ": " + lastStepResult);
                    log.info("{}: 已添加执行轨迹", getName());
                }
            }

            // =====================
            // 2️⃣ 判断结束原因
            // =====================
            String finishReason;

            if (getCurrentStep() >= getMaxSteps()) {
                finishReason = "达到最大步骤限制";
                this.state = AgentState.FINISHED;
                log.info("{}: 达到最大步骤限制", getName());
            } else if (state == AgentState.FINISHED) {
                finishReason = "任务正常完成";
                log.info("{}: 任务正常完成", getName());
            } else {
                finishReason = "未知结束";
                log.warn("{}: 未知结束原因", getName());
            }

            // =====================
            // 3️⃣ 🔥 总结阶段（核心升级）
            // =====================
            log.info("{}: 开始总结，结束原因: {}", getName(), finishReason);
            String finalResult = summarize(finishReason);
            log.info("{}: 总结完成，结果: {}", getName(), finalResult);

            // =====================
            // 4️⃣ 写入状态
            // =====================
            log.info("{}: 最终总结：{}", this.getName(), finalResult);
            
            // 从消息列表中提取原始的结构化结果
            String rawResult = extractRawResult(getMessageList());
            
            // 同时存储原始结果和总结结果（带版本）
            sharedState.put(this.getName(), finalResult, this.getName());
            // 如果有原始结构化结果，存储为结构化数据
            if (rawResult != null && !rawResult.isEmpty()) {
                sharedState.putStructuredData(this.getName(), rawResult, this.getName());
                log.info("{}: 已将原始结构化结果写入共享状态", getName());
            } else {
                sharedState.putStructuredData(this.getName(), finalResult, this.getName());
                log.info("{}: 已将总结结果写入共享状态（带版本）", getName());
            }

            // 创建AgentResult
            AgentResult result = new AgentResult();
            result.setSuccess(true);
            result.setFinalAnswer(finalResult);
            result.setActionType(AgentResult.ActionType.FINISH);
            
            return result;

        } catch (Exception e) {

            this.state = AgentState.ERROR;
            log.error("{}: 执行异常", getName(), e);

            // 🔥 错误也做总结（非常关键）
            String errorResult = summarize("执行异常: " + e.getMessage());
            log.info("{}: 错误总结: {}", getName(), errorResult);

            sharedState.put(this.getName(), errorResult, this.getName());
            sharedState.putStructuredData(this.getName(), errorResult, this.getName());
            log.info("{}: 已将错误结果写入共享状态（带版本）", getName());

            AgentResult result = new AgentResult();
            result.setSuccess(false);
            result.setFinalAnswer(errorResult);
            result.setActionType(AgentResult.ActionType.RETRY);
            
            return result;

        } finally {
            log.info("{}: 开始清理资源", getName());
            cleanup();
            log.info("{}: 资源清理完成", getName());
        }
    }

    /**
     * 思考过程
     * @return 是否需要执行行动
     */
    public abstract boolean think();

    /**
     * 执行行动
     * @return 行动结果
     */
    public abstract String act();

    /**
     * 总结过程
     * @param finishReason 结束原因
     * @return 总结结果
     */
    /**
     * 从消息列表中提取原始的结构化结果
     * @param messageList 消息列表
     * @return 原始结构化结果，如果没有找到则返回null
     */
    private String extractRawResult(java.util.List<org.springframework.ai.chat.messages.Message> messageList) {
        // 从后向前遍历消息列表，寻找可能的结构化结果
        for (int i = messageList.size() - 1; i >= 0; i--) {
            org.springframework.ai.chat.messages.Message message = messageList.get(i);
            if (message instanceof org.springframework.ai.chat.messages.AssistantMessage) {
                org.springframework.ai.chat.messages.AssistantMessage assistantMessage = (org.springframework.ai.chat.messages.AssistantMessage) message;
                String content = assistantMessage.getText();
                
                // 检查是否包含JSON格式的结果
                if (content != null) {
                    // 尝试提取JSON部分
                    int jsonStart = content.indexOf('{');
                    int jsonEnd = content.lastIndexOf('}');
                    if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                        String jsonContent = content.substring(jsonStart, jsonEnd + 1);
                        // 验证是否是有效的JSON
                        if (isValidJson(jsonContent)) {
                            log.info("{}: 提取到原始结构化结果", getName());
                            return jsonContent;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 验证字符串是否是有效的JSON
     * @param jsonString 要验证的字符串
     * @return 是否是有效的JSON
     */
    private boolean isValidJson(String jsonString) {
        try {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String summarize(String finishReason) {
        log.info("{}: 开始总结，结束原因: {}", getName(), finishReason);

        String result = "";
        try {

            String summaryPrompt = """
                你是一个总结专家。

                请根据以下Agent执行过程，总结最终结果。

                要求：
                1. 给出最终结论
                2. 简要说明执行过程
                3. 如果任务未完成，说明原因

                结束原因：%s

                """.formatted(finishReason);

            java.util.List<org.springframework.ai.chat.messages.Message> messages = new java.util.ArrayList<>(getMessageList());
            messages.add(new org.springframework.ai.chat.messages.UserMessage(summaryPrompt));
            log.info("{}: 已准备总结提示词", getName());

            result = getChatClient().prompt()
                    .messages(messages)
                    .call()
                    .content();
            log.info("{}: 总结生成完成", getName());

            return result;

        } catch (Exception e) {
            log.error("{}: 总结失败", getName(), e);

            // 降级策略（非常重要）
            return "任务执行完成，但总结失败。最后结果：".concat(result);
        }
    }
}
