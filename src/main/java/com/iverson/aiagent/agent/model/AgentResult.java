package com.iverson.aiagent.agent.model;

import lombok.Data;

import java.util.Map;

/**
 * 智能体执行结果
 * 统一返回结构，用于控制流程和传递数据
 */
@Data
public class AgentResult {

    /**
     * 执行是否成功
     */
    private boolean success;

    /**
     * 最终答案
     */
    private String finalAnswer;

    /**
     * 额外数据
     */
    private Map<String, Object> data;

    /**
     * 控制流程的动作类型
     */
    private ActionType actionType;

    /**
     * 目标智能体（当actionType为CALL_AGENT时使用）
     */
    private String targetAgent;

    /**
     * 动作类型枚举
     */
    public enum ActionType {
        /**
         * 完成任务
         */
        FINISH,
        /**
         * 重试任务
         */
        RETRY,
        /**
         * 调用其他智能体
         */
        CALL_AGENT,
        /**
         * 继续执行下一步
         */
        CONTINUE
    }
}
