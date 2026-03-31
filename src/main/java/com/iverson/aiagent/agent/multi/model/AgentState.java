package com.iverson.aiagent.agent.multi.model;

/**
 * @ClassName: AgentState
 * @Description: 代理执行状态的枚举类。
 * @Author: zhuze
 * @Date: 3/26/2026 5:16 PM
 * @Version: 1.0
 */
public enum AgentState {
    /**
     * 空闲状态
     */
    IDLE,
    /**
     * 运行中状态
     */
    RUNNING,
    /**
     * 完成状态
     */
    FINISHED,
    /**
     * 错误状态
     */
    ERROR
}
