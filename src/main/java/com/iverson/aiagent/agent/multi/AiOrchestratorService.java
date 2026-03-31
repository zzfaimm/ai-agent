package com.iverson.aiagent.agent.multi;


import com.iverson.aiagent.agent.multi.core.BaseAgent;
import com.iverson.aiagent.agent.multi.model.AgentResult;
import com.iverson.aiagent.agent.multi.model.SharedState;
import com.iverson.aiagent.agent.multi.planner.Plan;
import com.iverson.aiagent.agent.multi.planner.PlannerAgent;
import com.iverson.aiagent.agent.multi.planner.Task;
import com.iverson.aiagent.agent.multi.registry.AgentRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService executorService;

    public AiOrchestratorService(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
        this.executorService = Executors.newFixedThreadPool(5); // 创建一个固定大小的线程池
    }
    
    // 销毁方法，关闭线程池
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    /**
     * 评估任务重要性
     * @param taskName 任务名称
     * @return 是否为核心任务
     */
    private boolean isCriticalTask(String taskName) {
        // 核心任务列表
        List<String> criticalTasks = List.of("analysis", "copywriting", "seo");
        return criticalTasks.contains(taskName);
    }
    
    /**
     * 基于依赖关系为任务设置输入数据
     * @param currentTask 当前任务
     * @param state 共享状态
     * @param allTasks 所有任务列表
     */
    private void setInputDataForTask(Task currentTask, SharedState state, List<Task> allTasks) {
        // 获取当前任务的依赖
        List<String> dependencies = currentTask.getDependsOn();
        
        // 按照依赖顺序，尝试从最新的依赖任务中获取结构化数据
        for (String dependency : dependencies) {
            Object data = state.getStructuredData(dependency);
            if (data != null) {
                state.setCurrentContext(data.toString());
                System.out.println("为" + currentTask.getName() + "设置" + dependency + "的原始结果作为输入");
                return; // 找到第一个依赖的数据就使用
            }
        }
        
        // 如果没有找到依赖的数据，尝试从所有已执行的任务中获取最新的数据
        for (Task task : allTasks) {
            Object data = state.getStructuredData(task.getName());
            if (data != null) {
                state.setCurrentContext(data.toString());
                System.out.println("为" + currentTask.getName() + "设置" + task.getName() + "的原始结果作为输入");
                return;
            }
        }
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

            // 基于依赖关系为任务设置输入数据
            setInputDataForTask(currentTask, state, tasks);

            // 执行智能体
            System.out.println("开始执行智能体: " + agent.getName());
            AgentResult result;
            try {
                result = agent.run(state);
                System.out.println("智能体执行完成: " + agent.getName());
                System.out.println("执行结果: " + result.getActionType());
            } catch (Exception e) {
                System.err.println("执行智能体失败: " + currentTask.getName() + ", 错误: " + e.getMessage());
                if (isCriticalTask(currentTask.getName())) {
                    // 核心任务失败，需要重试
                    System.out.println("核心任务失败，需要重试: " + currentTask.getName());
                    taskQueue.add(currentTask);
                    continue;
                } else {
                    // 非核心任务失败，跳过
                    System.out.println("非核心任务失败，跳过: " + currentTask.getName());
                    executed.add(currentTask.getName());
                    addNextTasks(currentTask, tasks, executed, taskQueue);
                    continue;
                }
            }

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
                    
                    // 如果是需要重写的场景，执行完目标智能体后，再次执行当前任务进行重新评估
                    if (result.isNeedsRewrite()) {
                        System.out.println("需要重写，执行完 " + targetAgentName + " 后将重新评估");
                        taskQueue.add(currentTask);
                    }
                    break;
                case CONTINUE:
                    // 任务完成，继续执行下一步
                    executed.add(currentTask.getName());
                    System.out.println("任务完成，继续执行下一步: " + currentTask.getName());
                    // 添加后续任务
                    addNextTasks(currentTask, tasks, executed, taskQueue);
                    break;
                case CONDITIONAL:
                    // 条件分支
                    System.out.println("执行条件分支: " + result.getCondition());
                    // 这里简化处理，实际应该根据condition和sharedState进行评估
                    // 暂时默认条件为真
                    boolean conditionResult = true;
                    String conditionalTargetAgent = conditionResult ? result.getTrueTargetAgent() : result.getFalseTargetAgent();
                    if (conditionalTargetAgent != null) {
                        System.out.println("条件结果: " + conditionResult + "，执行智能体: " + conditionalTargetAgent);
                        Task conditionalTask = new Task(conditionalTargetAgent, new ArrayList<>());
                        taskQueue.add(conditionalTask);
                    }
                    // 继续执行当前任务
                    taskQueue.add(currentTask);
                    break;
                case WAIT:
                    // 等待其他任务完成
                    System.out.println("等待其他任务完成: " + currentTask.getName());
                    // 这里简化处理，实际应该检查依赖任务是否完成
                    // 暂时直接继续执行
                    taskQueue.add(currentTask);
                    break;
                case PARALLEL:
                    // 并行执行多个智能体
                    System.out.println("并行执行智能体: " + result.getParallelAgents());
                    if (result.getParallelAgents() != null && !result.getParallelAgents().isEmpty()) {
                        List<CompletableFuture<Void>> futures = new ArrayList<>();
                        
                        for (String parallelAgent : result.getParallelAgents()) {
                            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                                try {
                                    System.out.println("开始并行执行智能体: " + parallelAgent);
                                    BaseAgent parallelAgentInstance = agentRegistry.getAgent(parallelAgent);
                                    if (parallelAgentInstance != null) {
                                        parallelAgentInstance.run(state);
                                    }
                                    System.out.println("并行执行智能体完成: " + parallelAgent);
                                } catch (Exception e) {
                                    System.err.println("并行执行智能体失败: " + parallelAgent + ", 错误: " + e.getMessage());
                                }
                            }, executorService);
                            futures.add(future);
                        }
                        
                        // 等待所有并行任务完成
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                        System.out.println("所有并行智能体执行完成");
                    }
                    // 继续执行当前任务
                    taskQueue.add(currentTask);
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
