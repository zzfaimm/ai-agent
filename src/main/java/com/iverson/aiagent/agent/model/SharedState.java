package com.iverson.aiagent.agent.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🔥 核心：多Agent共享状态（类LangGraph State）
 */
@Data
public class SharedState {

    /** 原始用户输入 */
    private String userInput;

    /** 当前上下文（给下一个Agent用） */
    private String currentContext;

    /** 所有Agent执行结果 */
    private Map<String, Object> data = new HashMap<>();
    
    /** 结构化数据存储，用于传递JSON对象 */
    private Map<String, Object> structuredData = new HashMap<>();

    // 新增：执行轨迹（按顺序存储每个Agent的每一步）
    private List<String> traces = new ArrayList<>();

    /**
     * 添加执行轨迹
     * @param agentName Agent名称
     * @param log 步骤日志
     */
    public void addTrace(String agentName, String log) {
        traces.add(String.format("[%s] %s", agentName, log));
    }

    public void put(String key, Object value) {
        data.put(key, value);
        this.currentContext = value.toString();
    }
    
    /**
     * 存储结构化数据，用于传递JSON对象
     * @param key 键
     * @param value 结构化数据（如JSON对象）
     */
    public void putStructuredData(String key, Object value) {
        structuredData.put(key, value);
        // 同时更新currentContext，确保向后兼容
        this.currentContext = value.toString();
    }
    
    /**
     * 获取结构化数据
     * @param key 键
     * @return 结构化数据
     */
    public Object getStructuredData(String key) {
        return structuredData.get(key);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public boolean contains(String key) {
        return data.containsKey(key);
    }
    
    /**
     * 检查是否包含结构化数据
     * @param key 键
     * @return 是否包含
     */
    public boolean containsStructuredData(String key) {
        return structuredData.containsKey(key);
    }
    
    /**
     * 获取当前可用的结构化输入
     * 优先返回前一个Agent的结构化数据
     * @return 结构化输入
     */
    public Object getCurrentStructuredInput() {
        // 如果有结构化数据，返回最新的
        if (!structuredData.isEmpty()) {
            // 获取最后一个键
            String lastKey = (String) structuredData.keySet().toArray()[structuredData.size() - 1];
            return structuredData.get(lastKey);
        }
        // 否则返回用户输入
        return userInput;
    }
}