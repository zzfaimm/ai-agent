package com.iverson.aiagent.agent;


import com.iverson.aiagent.agent.base.BaseAgent;
import com.iverson.aiagent.agent.model.AgentResult;
import com.iverson.aiagent.agent.model.SharedState;
import com.iverson.aiagent.agent.planner.Plan;
import com.iverson.aiagent.agent.planner.PlannerAgent;
import com.iverson.aiagent.agent.planner.Task;
import com.iverson.aiagent.agent.registry.AgentRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Orchestrator（调度中心）
 *
 * 作用：
 * 1. 调用Planner生成任务
 * 2. 按计划执行Agent
 * 3. 根据AgentResult控制流程
 */
@Service
public class AiOrchestratorService {

    private final AgentRegistry agentRegistry;

    public AiOrchestratorService(AgentRegistry agentRegistry) {
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
        System.out.println("\n========================================");
        System.out.println("开始执行多智能体协作流程");
        System.out.println("========================================");
        
        // 1️⃣ 生成计划
        System.out.println("1. 生成执行计划...");
        BaseAgent plannerAgent = agentRegistry.getAgent("planner");
        Plan plan = ((PlannerAgent) plannerAgent).plan(input);
        System.out.println("执行计划生成完成，任务数量: " + plan.getTasks().size());
        System.out.println("任务列表:");
        for (Task task : plan.getTasks()) {
            System.out.println("  - " + task.getName() + " (依赖: " + task.getDependsOn() + ")");
        }

        // 2️⃣ 初始化状态
        System.out.println("\n2. 初始化共享状态...");
        SharedState state = new SharedState();
        state.setUserInput(input);
        state.setCurrentContext(input);
        System.out.println("共享状态初始化完成，用户输入: " + input);

        // 3️⃣ 任务执行（依赖驱动 + 结果驱动）
        System.out.println("\n3. 开始执行任务...");
        List<Task> tasks = plan.getTasks();
        List<String> executed = new ArrayList<>();
        Queue<Task> taskQueue = new LinkedBlockingQueue<>();
        
        // 初始化任务队列，添加所有初始任务（无依赖的任务）
        for (Task task : tasks) {
            if (task.getDependsOn().isEmpty()) {
                taskQueue.add(task);
                System.out.println("添加初始任务到队列: " + task.getName());
            }
        }

        while (!taskQueue.isEmpty()) {
            Task currentTask = taskQueue.poll();
            System.out.println("\n执行任务: " + currentTask.getName());
            
            BaseAgent agent = agentRegistry.getAgent(currentTask.getName());
            if (agent == null) {
                throw new RuntimeException("Agent不存在: " + currentTask.getName());
            }

            // 执行智能体
            System.out.println("开始执行智能体: " + agent.getName());
            AgentResult result = agent.run(state);
            System.out.println("智能体执行完成: " + agent.getName());
            System.out.println("执行结果: " + result.getActionType());

            // 根据执行结果决定下一步操作
            switch (result.getActionType()) {
                case FINISH:
                    // 任务完成，标记为已执行
                    executed.add(currentTask.getName());
                    System.out.println("任务完成: " + currentTask.getName());
                    // 添加后续任务
                    addNextTasks(currentTask, tasks, executed, taskQueue);
                    break;
                case RETRY:
                    // 任务失败，需要重试
                    System.out.println("任务需要重试: " + currentTask.getName());
                    taskQueue.add(currentTask);
                    break;
                case CALL_AGENT:
                    // 调用其他智能体
                    String targetAgentName = result.getTargetAgent();
                    System.out.println("调用其他智能体: " + targetAgentName);
                    Task targetTask = new Task(targetAgentName, new ArrayList<>());
                    taskQueue.add(targetTask);
                    // 执行完目标智能体后，继续执行当前任务
                    taskQueue.add(currentTask);
                    break;
                case CONTINUE:
                    // 任务完成，继续执行下一步
                    executed.add(currentTask.getName());
                    System.out.println("任务完成，继续执行下一步: " + currentTask.getName());
                    // 添加后续任务
                    addNextTasks(currentTask, tasks, executed, taskQueue);
                    break;
                default:
                    // 未知动作类型，标记为已执行
                    executed.add(currentTask.getName());
                    System.out.println("未知动作类型，任务标记为完成: " + currentTask.getName());
                    // 添加后续任务
                    addNextTasks(currentTask, tasks, executed, taskQueue);
                    break;
            }

            System.out.println("已完成任务数量: " + executed.size() + "/" + tasks.size());
            System.out.println("队列中任务数量: " + taskQueue.size());
        }

        System.out.println("\n========================================");
        System.out.println("多智能体协作流程执行完成");
        System.out.println("========================================");

        return state.getCurrentContext();
    }

    /**
     * 添加后续任务到队列
     */
    private void addNextTasks(Task completedTask, List<Task> allTasks, List<String> executed, Queue<Task> taskQueue) {
        for (Task task : allTasks) {
            // 跳过已执行的任务
            if (executed.contains(task.getName())) {
                continue;
            }
            
            // 检查任务是否依赖于已完成的任务
            if (task.getDependsOn().contains(completedTask.getName())) {
                // 检查所有依赖是否都已完成
                boolean allDependenciesCompleted = true;
                for (String dependency : task.getDependsOn()) {
                    if (!executed.contains(dependency)) {
                        allDependenciesCompleted = false;
                        break;
                    }
                }
                
                // 如果所有依赖都已完成，添加到队列
                if (allDependenciesCompleted) {
                    taskQueue.add(task);
                    System.out.println("添加后续任务到队列: " + task.getName());
                }
            }
        }
    }
}
