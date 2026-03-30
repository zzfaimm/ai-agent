package com.iverson.aiagent.agent.manyagent.model;

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
        CONTINUE,
        /**
         * 条件分支
         */
        CONDITIONAL,
        /**
         * 等待其他任务完成
         */
        WAIT,
        /**
         * 并行执行多个智能体
         */
        PARALLEL
    }
    
    /**
     * 条件分支的条件表达式
     */
    private String condition;
    
    /**
     * 条件为真时的目标智能体
     */
    private String trueTargetAgent;
    
    /**
     * 条件为假时的目标智能体
     */
    private String falseTargetAgent;
    
    /**
     * 并行执行的智能体列表
     */
    private java.util.List<String> parallelAgents;
    
    /**
     * 是否需要重写（当actionType为CALL_AGENT时使用，用于触发重写流程）
     */
    private boolean needsRewrite;
}
