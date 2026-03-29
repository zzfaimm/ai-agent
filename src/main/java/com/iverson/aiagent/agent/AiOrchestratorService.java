package com.iverson.aiagent.agent;


import com.iverson.aiagent.agent.base.BaseAgent;
import com.iverson.aiagent.agent.model.SharedState;
import com.iverson.aiagent.agent.planner.Plan;
import com.iverson.aiagent.agent.planner.PlannerAgent;
import com.iverson.aiagent.agent.planner.Task;
import com.iverson.aiagent.agent.registry.AgentRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrator（调度中心）
 *
 * 作用：
 * 1. 调用Planner生成任务
 * 2. 按计划执行Agent
 */
@Service
public class AiOrchestratorService {

    private final PlannerAgent plannerAgent;
    private final AgentRegistry agentRegistry;

    public AiOrchestratorService(PlannerAgent plannerAgent,
                                 AgentRegistry agentRegistry) {
        this.plannerAgent = plannerAgent;
        this.agentRegistry = agentRegistry;
    }

    /**
     * 执行AI流程
     */
//    public String run(String input) {
//
//        // 1️⃣ 生成执行计划
//        Plan plan = plannerAgent.plan(input);
//
//        String context = input;
//
//        // 2️⃣ 动态执行
//        for (Task task : plan.getTasks()) {
//
//            BaseAgent agent = agentRegistry.getAgent(task.getName());
//
//            if (agent == null) {
//                throw new RuntimeException("Agent不存在: " + task);
//            }
//
//            context = agent.run(context);
//        }
//
//        return context;
//    }

    public String run(String input) {

        // 1️⃣ 生成计划
        Plan plan = plannerAgent.plan(input);

        // 2️⃣ 初始化状态
        SharedState state = new SharedState();
        state.setUserInput(input);
        state.setCurrentContext(input);

        // 3️⃣ 任务执行（依赖驱动）
        List<Task> tasks = plan.getTasks();
        List<String> executed = new ArrayList<>();

        while (executed.size() < tasks.size()) {

            for (Task task : tasks) {

                if (executed.contains(task.getName())) {
                    continue;
                }

                // ✅ 判断依赖是否完成
                if (!executed.containsAll(task.getDependsOn())) {
                    continue;
                }

                BaseAgent agent = agentRegistry.getAgent(task.getName());

                if (agent == null) {
                    throw new RuntimeException("Agent不存在: " + task.getName());
                }

                // 🔥 执行
                agent.run(state);

                executed.add(task.getName());
            }
        }

        return state.getCurrentContext();
    }
}