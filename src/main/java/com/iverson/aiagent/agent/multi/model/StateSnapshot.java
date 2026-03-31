package com.iverson.aiagent.agent.multi.model;

import lombok.Data;

/**
 * 状态快照
 * 用于记录共享状态的历史版本，支持回溯和审计
 */
@Data
public class StateSnapshot {

    /**
     * 状态值
     */
    private Object value;

    /**
     * 修改状态的智能体名称
     */
    private String agent;

    /**
     * 修改时间戳
     */
    private long timestamp;

    /**
     * 构造函数
     * @param value 状态值
     * @param agent 智能体名称
     */
    public StateSnapshot(Object value, String agent) {
        this.value = value;
        this.agent = agent;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构造函数
     * @param value 状态值
     * @param agent 智能体名称
     * @param timestamp 时间戳
     */
    public StateSnapshot(Object value, String agent, long timestamp) {
        this.value = value;
        this.agent = agent;
        this.timestamp = timestamp;
    }
}
