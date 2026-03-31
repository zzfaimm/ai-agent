package com.iverson.aiagent.agent.multi.registry;


import com.iverson.aiagent.agent.multi.core.BaseAgent;
import com.iverson.aiagent.agent.multi.planner.AgentDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Agent注册中心（企业级核心设计）
 *
 * 作用：
 * 1. 管理所有Agent
 * 2. 提供动态扩展能力（新增Agent无需改核心代码）
 */
@Component
public class AgentRegistry {

    // 存储工厂函数
    private final Map<String, Supplier<BaseAgent>> agentFactoryMap = new HashMap<>();

    // 存储 Agent 定义（用于动态 Prompt）
    private final Map<String, AgentDefinition> agentDefinitionMap = new HashMap<>();

    /**
     * 注册 Agent（同时存储工厂和定义）
     * @param name Agent 名称
     * @param factory 工厂函数
     * @param definition Agent 定义信息
     */
    public void register(String name, Supplier<BaseAgent> factory, AgentDefinition definition) {
        agentFactoryMap.put(name, factory);
        agentDefinitionMap.put(name, definition);
    }

    /**
     * 获取 Agent 实例（每次调用创建新实例）
     * @param name Agent 名称
     * @return Agent 实例
     */
    public BaseAgent getAgent(String name) {
        Supplier<BaseAgent> factory = agentFactoryMap.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("Agent not registered: " + name);
        }
        return factory.get();
    }

    /**
     * 获取所有 Agent 定义列表（用于 Planner 构建 Prompt）
     * @return Agent 定义列表
     */
    public List<AgentDefinition> getAgentDefinitions() {
        return new ArrayList<>(agentDefinitionMap.values());
    }

    /**
     * 检查 Agent 是否存在
     */
    public boolean containsAgent(String name) {
        return agentFactoryMap.containsKey(name);
    }
}