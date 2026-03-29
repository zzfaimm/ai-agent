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
    
    // 新增：版本化状态存储，支持回溯和审计
    private Map<String, List<StateSnapshot>> stateMap = new HashMap<>();

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
     * 存储带版本的状态数据
     * @param key 键
     * @param value 值
     * @param agentName 智能体名称
     */
    public void put(String key, Object value, String agentName) {
        // 存储到普通数据中
        data.put(key, value);
        // 存储到版本化状态中
        stateMap.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new StateSnapshot(value, agentName));
        // 更新当前上下文
        this.currentContext = value.toString();
    }
    
    /**
     * 存储带版本的结构化数据
     * @param key 键
     * @param value 结构化数据
     * @param agentName 智能体名称
     */
    public void putStructuredData(String key, Object value, String agentName) {
        // 存储到结构化数据中
        structuredData.put(key, value);
        // 存储到版本化状态中
        stateMap.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new StateSnapshot(value, agentName));
        // 更新当前上下文
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
    
    /**
     * 获取最新的状态值
     * @param key 键
     * @return 最新的状态值
     */
    public Object getLatest(String key) {
        List<StateSnapshot> list = stateMap.get(key);
        return list == null ? null : list.get(list.size() - 1).getValue();
    }
    
    /**
     * 获取状态的历史版本
     * @param key 键
     * @return 状态的历史版本列表
     */
    public List<StateSnapshot> getHistory(String key) {
        return stateMap.getOrDefault(key, new ArrayList<>());
    }
    
    /**
     * 获取状态的特定版本
     * @param key 键
     * @param index 版本索引
     * @return 特定版本的状态值
     */
    public Object getVersion(String key, int index) {
        List<StateSnapshot> list = stateMap.get(key);
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index).getValue();
    }
    
    /**
     * 回滚到状态的特定版本
     * @param key 键
     * @param index 版本索引
     * @return 是否回滚成功
     */
    public boolean rollbackToVersion(String key, int index) {
        List<StateSnapshot> list = stateMap.get(key);
        if (list == null || index < 0 || index >= list.size()) {
            return false;
        }
        StateSnapshot snapshot = list.get(index);
        data.put(key, snapshot.getValue());
        if (structuredData.containsKey(key)) {
            structuredData.put(key, snapshot.getValue());
        }
        this.currentContext = snapshot.getValue().toString();
        return true;
    }
}