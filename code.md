package com.iverson.aiagent.agent.base;

import com.iverson.aiagent.agent.model.AgentState;
import com.iverson.aiagent.agent.model.SharedState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.util.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
* @ClassName: BaseAgent
* @Description: 抽象基础代理类，用于管理代理状态和执行流程。
* 提供状态转换，内存管理，基于步骤执行循环的基础功能。子类必须实现step方法。
* @Author: zhuze
* @Date: 3/26/2026 5:16 PM
* @Version: 1.0
  */
  @Data
  @Slf4j
  public abstract class BaseAgent {

  // 核心属性
  private String name;

  // 提示
  private String systemPrompt;
  private String nextStepPrompt;

  // 多Agent之间的数据通信
  protected SharedState sharedState;

  // 状态
  private AgentState state = AgentState.IDLE;

  // 执行控制
  private int maxSteps = 10;
  private int currentStep = 0;

  // LLM
  private ChatClient chatClient;

  // Memory(需要自主维护上下文)
  private List<Message> messageList = new ArrayList<>();

  /**
    * 运行代理（流式输出）
    *
    * @param userPrompt 用户提示词
    * @return SseEmitter实例
      */
      public SseEmitter runStream(String userPrompt) {
      log.info("{}: 开始流式执行，用户输入: {}", getName(), userPrompt);
      // 创建SseEmitter，设置较长的超时时间
      SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

      // 使用线程异步处理，避免阻塞主线程
      CompletableFuture.runAsync(() -> {
      try {
      if (this.state != AgentState.IDLE) {
      log.warn("{}: 无法从状态运行代理: {}", getName(), this.state);
      emitter.send("错误：无法从状态运行代理: " + this.state);
      emitter.complete();
      return;
      }
      if (StringUtil.isEmpty(userPrompt)) {
      log.warn("{}: 空提示词", getName());
      emitter.send("错误：不能使用空提示词运行代理");
      emitter.complete();
      return;
      }

               // 更改状态
               state = AgentState.RUNNING;
               log.info("{}: 状态变更为: {}", getName(), state);
               // 记录消息上下文
               messageList.add(new UserMessage(userPrompt));
               log.info("{}: 已添加用户消息到上下文", getName());

               try {
                   for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                       int stepNumber = i + 1;
                       currentStep = stepNumber;
                       log.info("{}: 执行步骤 {}/{}", getName(), stepNumber, maxSteps);

                       // 单步执行
                       String stepResult = step();
                       String result = "Step " + stepNumber + ": " + stepResult;
                       log.info("{}: 步骤 {} 结果: {}", getName(), stepNumber, stepResult);

                       // 发送每一步的结果
                       emitter.send(result);
                   }
                   // 检查是否超出步骤限制
                   if (currentStep >= maxSteps) {
                       state = AgentState.FINISHED;
                       log.info("{}: 达到最大步骤限制，执行结束", getName());
                       emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                   } else {
                       log.info("{}: 任务正常完成", getName());
                   }
                   // 正常完成
                   emitter.complete();
               } catch (Exception e) {
                   state = AgentState.ERROR;
                   log.error("{}: 执行智能体失败", getName(), e);
                   try {
                       emitter.send("执行错误: " + e.getMessage());
                       emitter.complete();
                   } catch (Exception ex) {
                       emitter.completeWithError(ex);
                   }
               } finally {
                   // 清理资源
                   log.info("{}: 开始清理资源", getName());
                   this.cleanup();
                   log.info("{}: 资源清理完成", getName());
               }
           } catch (Exception e) {
               log.error("{}: 流式执行异常", getName(), e);
               emitter.completeWithError(e);
           }
      });

      // 设置超时和完成回调
      emitter.onTimeout(() -> {
      this.state = AgentState.ERROR;
      this.cleanup();
      log.warn("{}: SSE连接超时", getName());
      });

      emitter.onCompletion(() -> {
      if (this.state == AgentState.RUNNING) {
      this.state = AgentState.FINISHED;
      }
      this.cleanup();
      log.info("{}: SSE连接完成", getName());
      });

      return emitter;
      }


    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt){
        log.info("{}: 开始执行，用户输入: {}", getName(), userPrompt);
        // 1.做校验
        if (this.state != AgentState.IDLE){
            log.warn("{}: 无法从状态运行代理: {}", getName(), this.state);
            throw new RuntimeException("cannot run agent from state " + this.state);
        }
        if (StringUtil.isEmpty(userPrompt)){
            log.warn("{}: 空提示词", getName());
            throw new RuntimeException("userPrompt is empty");
        }
        //更改状态
        state = AgentState.RUNNING;
        log.info("{}: 状态变更为: {}", getName(), state);
        //记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        log.info("{}: 已添加用户消息到上下文", getName());
        //保存结果列表
        List<String> results = new ArrayList<>();

        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++){
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("{}: 执行步骤 {}/{}", getName(), stepNumber, maxSteps);
                //单步执行
                String stepResult = step();
                String result = "step " + stepNumber + " of " + stepResult;
                results.add(result);
                log.info("{}: 步骤 {} 结果: {}", getName(), stepNumber, stepResult);
            }
            //检查是否超出步骤限制
            if (currentStep >= maxSteps){
                state = AgentState.FINISHED;
                log.info("{}: 达到最大步骤限制，执行结束", getName());
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            } else {
                log.info("{}: 任务正常完成", getName());
            }
            String finalResult = String.join("\n", results);
            log.info("{}: 执行结果: {}", getName(), finalResult);
            return finalResult;
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("{}: 执行智能体失败", getName(), e);
            return "error" + e.getMessage();
        } finally {
            //清理资源
            log.info("{}: 开始清理资源", getName());
            this.cleanup();
            log.info("{}: 资源清理完成", getName());
        }
    }


    public String run(SharedState sharedState) {
        log.info("{}: 开始执行，共享状态: {}", getName(), sharedState);

        if (this.state != AgentState.IDLE) {
            log.warn("{}: 无法从状态运行代理: {}", getName(), this.state);
            throw new RuntimeException("Agent状态错误: " + this.state);
        }

        this.state = AgentState.RUNNING;
        log.info("{}: 状态变更为: {}", getName(), state);
        this.sharedState = sharedState;
        log.info("{}: 已设置共享状态", getName());

        // 优先使用结构化输入
        Object structuredInput = sharedState.getCurrentStructuredInput();
        String input = structuredInput != null ? structuredInput.toString() : sharedState.getUserInput();
        log.info("{}: 使用输入: {}", getName(), input);
        
        if (input == null) {
            input = sharedState.getUserInput();
            log.info("{}: 使用用户输入: {}", getName(), input);
        }

        messageList.add(new UserMessage(input));
        log.info("{}: 已添加输入到消息上下文", getName());

        String lastStepResult = null;

        try {

            // =====================
            // 1️⃣ 执行阶段
            // =====================
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {

                currentStep = i + 1;
                log.info("{}: 执行步骤 {}/{}", getName(), currentStep, maxSteps);

                String stepResult = step();
                log.info("{}: 步骤 {} 结果: {}", getName(), currentStep, stepResult);

                lastStepResult = stepResult;

                // 🔥 记录执行轨迹
                if("思考完成 - 无需行动".equalsIgnoreCase(stepResult)) {
                    sharedState.addTrace(this.name,
                            "Step " + currentStep + ": " + stepResult);
                    log.info("{}: 已添加执行轨迹", getName());
                }
            }

            // =====================
            // 2️⃣ 判断结束原因
            // =====================
            String finishReason;

            if (currentStep >= maxSteps) {
                finishReason = "达到最大步骤限制";
                this.state = AgentState.FINISHED;
                log.info("{}: 达到最大步骤限制", getName());
            } else if (state == AgentState.FINISHED) {
                finishReason = "任务正常完成";
                log.info("{}: 任务正常完成", getName());
            } else {
                finishReason = "未知结束";
                log.warn("{}: 未知结束原因", getName());
            }

            // =====================
            // 3️⃣ 🔥 总结阶段（核心升级）
            // =====================
            log.info("{}: 开始总结，结束原因: {}", getName(), finishReason);
            String finalResult = summarize(finishReason);
            log.info("{}: 总结完成，结果: {}", getName(), finalResult);

            // =====================
            // 4️⃣ 写入状态
            // =====================
            log.info("{}: 最终总结：{}", this.name, finalResult);
            
            // 同时存储结构化数据和普通数据
            sharedState.put(this.name, finalResult);
            sharedState.putStructuredData(this.name, finalResult);
            log.info("{}: 已将结果写入共享状态", getName());

            return finalResult;

        } catch (Exception e) {

            this.state = AgentState.ERROR;
            log.error("{}: 执行异常", getName(), e);

            // 🔥 错误也做总结（非常关键）
            String errorResult = summarize("执行异常: " + e.getMessage());
            log.info("{}: 错误总结: {}", getName(), errorResult);

            sharedState.put(this.name, errorResult);
            sharedState.putStructuredData(this.name, errorResult);
            log.info("{}: 已将错误结果写入共享状态", getName());

            return errorResult;

        } finally {
            log.info("{}: 开始清理资源", getName());
            cleanup();
            log.info("{}: 资源清理完成", getName());
        }
    }

    private String summarize(String finishReason) {
        log.info("{}: 开始总结，结束原因: {}", getName(), finishReason);

        String result = "";
        try {

            String summaryPrompt = """
                你是一个总结专家。

                请根据以下Agent执行过程，总结最终结果。

                要求：
                1. 给出最终结论
                2. 简要说明执行过程
                3. 如果任务未完成，说明原因

                结束原因：%s

                """.formatted(finishReason);

            List<Message> messages = new ArrayList<>(this.messageList);
            messages.add(new UserMessage(summaryPrompt));
            log.info("{}: 已准备总结提示词", getName());

            result = chatClient.prompt()
                    .messages(messages)
                    .call()
                    .content();
            log.info("{}: 总结生成完成", getName());

            return result;

        } catch (Exception e) {
            log.error("{}: 总结失败", getName(), e);

            // 降级策略（非常重要）
            return "任务执行完成，但总结失败。最后结果：".concat(result);
        }
    }

    /**
     *
     * @param
     * @return 步骤执行结果
     */
    public abstract String step();

    /**
     *
     * @param
     * @return 清理资源
     */
    protected void cleanup(){
        log.info("{}: 执行清理操作", getName());
    }
}

package com.iverson.aiagent.agent.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
* @ClassName: ReActAgent
* @Description: TODO(描述这个类的作用)
* @Author: zhuze
* @Date: 3/26/2026 6:13 PM
* @Version: 1.0
  */
  @Data
  @EqualsAndHashCode(callSuper = true)
  public abstract class ReActAgent extends BaseAgent {
  /**
  *处理当前状态并决定下一步行动
    * @param
    * @return 是否需要执行行动，true表示需要执行，false表示不需要执行
      */
      public abstract boolean think();

  /**
  *执行决定要执行的行动
    * @param
    * @return 行动执行的结果
      */
      public abstract String act();


    /**
     * 执行单个步骤：思考和行动
     * @param
     * @return 步骤执行结果
     */

    @Override
    public String step(){
        try {
            boolean shouldAct = think();
            if (!shouldAct){
                return "思考完成 - 无需行动";
            }
            return act();
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            return "步骤执行失败 ： " + e.getMessage();
        }
    }


}


package com.iverson.aiagent.agent.base;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.iverson.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* @ClassName: ToolCallAgent
* @Description: 处理工具调用的基础类，具体实现 think 和 act 方法，可以用作创建实例的父类
* @Author: zhuze
* @Date: 3/26/2026 6:22 PM
* @Version: 1.0
  */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @Slf4j
  public class ToolCallAgent extends ReActAgent {

  //可用的工具
  private final ToolCallback[] availableTools;

  // 保存工具调用响应信息
  private ChatResponse toolCallChatResponse;

  // 工具调用的管理者
  private final ToolCallingManager toolCallingManager;

  private static final int MAX_TOOL_RESULT_LEN = 5000;

  // 禁止内置的工具调用机制运行，自己维护上下文
  private final ChatOptions chatOptions;

  public ToolCallAgent(ToolCallback[] availableTools){
  super();
  this.availableTools = availableTools;
  this.toolCallingManager = ToolCallingManager.builder().build();
  // 禁止 Spring AI 内置的工具调用机制，自己维护选项和上下文
  this.chatOptions = DashScopeChatOptions.builder()
  .withProxyToolCalls(true)
  .build();
  log.info("{}: 初始化完成，可用工具数量: {}", getName(), availableTools.length);
  }

  /**
    * 处理当前状态并且决定下一步行动
    * @param
    * @return 是否需要执行工具
      */
      @Override
      public boolean think() {
      log.info("{}: 开始思考步骤", getName());
      if (getSystemPrompt() != null && !getSystemPrompt().isEmpty()) {
      SystemMessage systemMessage = new SystemMessage(getSystemPrompt());
      getMessageList().add(systemMessage);
      log.info("{}: 已添加系统提示词到上下文", getName());
      }
      if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
      UserMessage userMessage = new UserMessage(getNextStepPrompt());
      getMessageList().add(userMessage);
      log.info("{}: 已添加下一步提示词到上下文", getName());
      }
      List<Message> messageList =  getMessageList();
      Prompt prompt = new Prompt(messageList, chatOptions);
      log.info("{}: 已准备思考提示词，消息数量: {}", getName(), messageList.size());

      try {
      // 获取带工具选项的响应
      log.info("{}: 开始调用LLM进行思考", getName());
      ChatResponse chatResponse = getChatClient().prompt(prompt)
      .system(getSystemPrompt())
      .tools(availableTools)
      .call()
      .chatResponse();
      // 记录响应， 用于 Act
      this.toolCallChatResponse = chatResponse;
      AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
      // 输出提示信息
      String result = assistantMessage.getText();
      List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
      log.info("{}的思考{}", getName(), result);
      log.info("{}: 选择了{}个工具来使用", getName(), toolCallList.size());
      String toolCallInfo = toolCallList.stream()
      .map(toolCall -> String.format("工具名称：%s, 参数：%s",
      toolCall.name(), toolCall.arguments()))
      .collect(Collectors.joining("\n"));
      log.info(toolCallInfo);
      if (toolCallList.isEmpty()) {
      // 只有不调用工具时，才需要记录助手消息
      getMessageList().add(assistantMessage);
      sharedState.addTrace(getName(),
      "Step " + getCurrentStep() + ": " + assistantMessage);
      log.info("{}: 未调用工具，已记录助手消息", getName());
      //                getSharedState().put(getName(), assistantMessage);
      return false;
      }else {
      // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
      log.info("{}: 准备调用工具", getName());
      return true;
      }
      } catch (Exception e) {
      log.error("{}: 思考过程遇到了问题: {}", getName(), e.getMessage(), e);
      getMessageList().add(new AssistantMessage("think方法时遇到错误" + e.getMessage()));
      return false;
      }
      }

  /**
    * 执行工具调用并且处理结果
    * @param
    * @return 执行结果
      */
      //    @Override
      //    public String act() {
      //        if (!toolCallChatResponse.hasToolCalls()){
      //            return "没有工具调用";
      //        }
      //        // 调用工具
      //        Prompt prompt = new Prompt(getMessageList(), chatOptions);
      //        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
      //        // 记录消息上下文，conversationHistory方法已经包含了助手消息和工具调用返回的结果
      //        setMessageList(toolExecutionResult.conversationHistory());
      //        // 当前工具调用的结果
      //        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
      //        String result = toolResponseMessage.getResponses().stream()
      //                .map(response -> "工具" + response.name() + "完成了他的任务！结果: " + response.responseData())
      //                .collect(Collectors.joining("\n"));
      //        // 判断是否调用了终止工具
      //        boolean doTerminateToolCalled = toolResponseMessage.getResponses().stream()
      //                .anyMatch(response -> "doTerminate".equals(response.name()));
      //        if (doTerminateToolCalled) {
      //            setState(AgentState.FINISHED);
      //        }
      //        log.info(result);
      //        return result;
      //    }

  @Override
  public String act() {
  log.info("{}: 开始执行工具调用", getName());
  if (!toolCallChatResponse.hasToolCalls()){
  log.info("{}: 没有工具调用", getName());
  return "没有工具调用";
  }

       // 1. 执行工具调用，获得结果
       Prompt prompt = new Prompt(getMessageList(), chatOptions);
       log.info("{}: 准备执行工具调用，消息数量: {}", getName(), getMessageList().size());
       ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
       log.info("{}: 工具调用执行完成", getName());

       // 2. 🔥 对工具返回的消息进行截断处理
       List<Message> originalHistory = toolExecutionResult.conversationHistory();
       List<Message> truncatedHistory = new ArrayList<>(originalHistory.size());
       log.info("{}: 开始处理工具返回结果，原始消息数量: {}", getName(), originalHistory.size());

       for (Message msg : originalHistory) {
           if (msg instanceof ToolResponseMessage) {
               // 只处理 ToolResponseMessage，其他消息原样保留
               ToolResponseMessage trm = (ToolResponseMessage) msg;
               List<ToolResponseMessage.ToolResponse> originalResponses = trm.getResponses();
               List<ToolResponseMessage.ToolResponse> truncatedResponses = originalResponses.stream()
                       .map(response -> {
                           String data = response.responseData();
                           if (data != null && data.length() > MAX_TOOL_RESULT_LEN) {
                               data = data.substring(0, MAX_TOOL_RESULT_LEN) + " ... [内容过长已截断]";
                               log.info("{}: 工具返回结果已截断", getName());
                           }
                           // 创建新的 ToolResponse（保持原名称和截断后的数据）
                           return new ToolResponseMessage.ToolResponse(response.id(), response.name(), data);
                       })
                       .collect(Collectors.toList());
               // 创建新的 ToolResponseMessage，元数据保持不变
               ToolResponseMessage truncatedTrm = new ToolResponseMessage(truncatedResponses, trm.getMetadata());
               truncatedHistory.add(truncatedTrm);
           } else {
               truncatedHistory.add(msg);
           }
       }

       // 3. 保存截断后的消息列表
       setMessageList(truncatedHistory);
       log.info("{}: 已保存截断后的消息列表，消息数量: {}", getName(), truncatedHistory.size());

       // 4. 提取工具执行结果（用于日志和返回，这里也使用截断后的版本）
       ToolResponseMessage lastToolMsg = (ToolResponseMessage) CollUtil.getLast(truncatedHistory);
       String result = lastToolMsg.getResponses().stream()
               .map(response -> "工具" + response.name() + "完成了他的任务！结果: " + response.responseData())
               .collect(Collectors.joining("\n"));
       log.info("{}: 工具执行结果: {}", getName(), result);

       // 5. 判断终止工具（与原来一致）
       boolean doTerminateToolCalled = lastToolMsg.getResponses().stream()
               .anyMatch(response -> "doTerminate".equals(response.name()));
       if (doTerminateToolCalled) {
           setState(AgentState.FINISHED);
           log.info("{}: 调用了终止工具，任务结束", getName());
       }

       log.info(result);
       return result;
  }
  }




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
System.out.println("\n========================================");
System.out.println("开始执行多智能体协作流程");
System.out.println("========================================");

     // 1️⃣ 生成计划
     System.out.println("1. 生成执行计划...");
     Plan plan = plannerAgent.plan(input);
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

     // 3️⃣ 任务执行（依赖驱动）
     System.out.println("\n3. 开始执行任务...");
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

             System.out.println("\n执行任务: " + task.getName());
             System.out.println("依赖任务: " + task.getDependsOn());
             
             BaseAgent agent = agentRegistry.getAgent(task.getName());

             if (agent == null) {
                 throw new RuntimeException("Agent不存在: " + task.getName());
             }

             // 🔥 执行
             System.out.println("开始执行智能体: " + agent.getName());
             agent.run(state);
             System.out.println("智能体执行完成: " + agent.getName());

             executed.add(task.getName());
             System.out.println("已完成任务数量: " + executed.size() + "/" + tasks.size());
         }
     }

     System.out.println("\n========================================");
     System.out.println("多智能体协作流程执行完成");
     System.out.println("========================================");

     return state.getCurrentContext();
}
}



package com.iverson.aiagent.agent.planner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iverson.aiagent.agent.base.BaseAgent;
import com.iverson.aiagent.agent.registry.AgentRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.model.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* PlannerAgent（核心大脑）
*
* 作用：
* 1. 根据用户输入决定执行哪些Agent
* 2. 动态生成任务流程（而不是写死）
     */
     @Component
     @Slf4j
     public class PlannerAgent extends BaseAgent {

private final AgentRegistry agentRegistry;
private final ObjectMapper objectMapper = new ObjectMapper();

public PlannerAgent(ChatModel dashscopeChatModel, AgentRegistry agentRegistry) {
this.agentRegistry = agentRegistry;
this.setChatClient(ChatClient.builder(dashscopeChatModel).build());
}

public Plan plan(String userInput) {
String systemPrompt = buildDynamicPrompt();
String userPrompt = "用户需求：" + userInput + "\n请生成任务计划。";

     String response = getChatClient().prompt()
             .system(systemPrompt)
             .user(userPrompt)
             .call()
             .content();
     log.info(response);
     return parsePlan(response);
}

private String buildDynamicPrompt() {
String agentInfo = agentRegistry.getAgentDefinitions().stream()
.map(def -> String.format("""
- 名称: %s
描述: %s
能力详情: %s
输入: %s
输出: %s
""", def.getName(), def.getDescription(), def.getDetail(),
String.join(", ", def.getRequiredInputs()),
String.join(", ", def.getOutputs())))
.collect(Collectors.joining("\n"));

     return """
         你是一个任务规划专家。根据用户需求，选择合适的Agent并安排执行顺序。

         ## 可用Agent
         %s

         ## 规划规则
         - 如果某个Agent的输入依赖前一个Agent的输出，请在dependsOn中指明。
         - 如果任务可以并行执行，请标注parallelGroups。
         - 只返回JSON，不要有其他文字。

         ## 返回格式示例
         {
           "tasks": [
             {"name": "analysis", "dependsOn": []},
             {"name": "copywriting", "dependsOn": ["analysis"]}
           ]
         }

         """.formatted(agentInfo);
}

private Plan parsePlan(String response) {
try {
JsonNode root = objectMapper.readTree(response);
List<Task> tasks = new ArrayList<>();
JsonNode tasksNode = root.get("tasks");
if (tasksNode.isArray()) {
for (JsonNode t : tasksNode) {
String name = t.get("name").asText();
List<String> dependsOn = new ArrayList<>();
JsonNode dependsNode = t.get("dependsOn");
if (dependsNode.isArray()) {
for (JsonNode d : dependsNode) {
dependsOn.add(d.asText());
}
}
tasks.add(new Task(name, dependsOn));
}
}
// 可选：解析parallelGroups
return new Plan(tasks);
} catch (Exception e) {
log.error("Failed to parse plan: {}", response, e);
// 降级：返回默认顺序
List<Task> defaultTasks = List.of(
new Task("analysis", List.of()),
new Task("copywriting", List.of("analysis")),
new Task("seo", List.of("copywriting"))
);
return new Plan(defaultTasks);
}
}

@Override
public String step() {
return null;
}
}


package com.iverson.aiagent.agent.planner;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AgentDefinition {
private String name;          // 唯一标识
private String displayName;   // 展示名称
private String description;   // 简短描述（一句话）
private String detail;        // 详细能力、输入输出说明
private List<String> requiredInputs; // 需要的输入信息
private List<String> outputs;        // 输出内容
}
package com.iverson.aiagent.agent.planner;

import lombok.Data;

import java.util.List;

@Data
public class Plan {
private List<Task> tasks;  // Task 包含 name 和 dependsOn
// 可选：并行执行的分组
private List<List<String>> parallelGroups;

    public Plan(List<Task> tasks) {
        this.tasks = tasks;
    }
}
package com.iverson.aiagent.agent.planner;

import lombok.Data;

import java.util.List;

@Data
public class Task {
private String name;
private List<String> dependsOn;

    public Task(String name, List<String> dependsOn) {
        this.name = name;
        this.dependsOn = dependsOn;
    }
}


package com.iverson.aiagent.agent.registry;


import com.iverson.aiagent.agent.base.BaseAgent;
import com.iverson.aiagent.agent.planner.AgentDefinition;
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


package com.iverson.aiagent.agent.registry;

import com.iverson.aiagent.agent.impl.AnalysisAgent;
import com.iverson.aiagent.agent.impl.CopywritingAgent;
import com.iverson.aiagent.agent.impl.SeoAgent;
import com.iverson.aiagent.agent.planner.AgentDefinition;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AgentFactoryConfig {

    @Autowired
    private ToolCallback[] allTools;

    @Autowired
    private ChatModel dashscopeChatModel;

    @Autowired
    private AgentRegistry registry;

    @PostConstruct
    public void registerAgents() {
        // 注册 AnalysisAgent
        registry.register("analysis",
                () -> {
                    AnalysisAgent agent = new AnalysisAgent(allTools);
                    agent.setChatClient(ChatClient.builder(dashscopeChatModel).build());
                    agent.setMaxSteps(10);
                    return agent;
                },
                new AgentDefinition(
                        "analysis",
                        "商品分析",
                        "分析商品的目标用户、核心卖点、使用场景，输出结构化的分析结果（JSON格式）。",
                        "输入：商品名称和描述（文本）。输出：包含目标用户画像、核心卖点列表、典型使用场景的JSON对象。",
                        List.of("商品描述"),
                        List.of("分析结果JSON")
                )
        );

        // 注册 CopywritingAgent
        registry.register("copywriting",
                () -> {
                    CopywritingAgent agent = new CopywritingAgent(allTools);
                    agent.setChatClient(ChatClient.builder(dashscopeChatModel).build());
                    agent.setMaxSteps(10);
                    return agent;
                },
                new AgentDefinition(
                        "copywriting",
                        "文案生成",
                        "根据商品分析结果生成吸引人的商品标题、详细描述和卖点文案。",
                        "输入：商品分析结果（JSON格式，包含目标用户、卖点、场景）。输出：包含标题、描述、卖点文案的JSON对象。",
                        List.of("分析结果"),
                        List.of("文案JSON")
                )
        );

        // 注册 SeoAgent
        registry.register("seo",
                () -> {
                    SeoAgent agent = new SeoAgent(allTools);
                    agent.setChatClient(ChatClient.builder(dashscopeChatModel).build());
                    agent.setMaxSteps(10);
                    return agent;
                },
                new AgentDefinition(
                        "seo",
                        "SEO优化",
                        "提取关键词，优化标题和描述，提升搜索引擎排名。",
                        "输入：商品名称、分析结果、文案初稿（文本）。输出：包含关键词列表、优化后标题、优化后描述的JSON对象。",
                        List.of("商品名称", "分析结果", "文案"),
                        List.of("SEO优化结果JSON")
                )
        );
    }
}

package com.iverson.aiagent.agent.impl;


import com.iverson.aiagent.agent.base.ToolCallAgent;
import org.springframework.ai.tool.ToolCallback;

/**
* 商品分析Agent
  */

public class AnalysisAgent extends ToolCallAgent {

    public AnalysisAgent(ToolCallback[] tools) {

        super(tools);

        this.setName("analysis");

        this.setSystemPrompt("""
        你是一个电商商品分析专家。你的输入是用户提供的商品名称和描述。
        
        请严格按照以下步骤进行分析，并输出 JSON 格式的结果：
        1. 确定目标用户群体（年龄、职业、使用习惯等）
        2. 提炼核心卖点（3-5 个关键优势）
        3. 列举典型使用场景（3 个以上）
        
        输出格式示例：
        {
          "targetAudience": "...",
          "sellingPoints": ["...", "...", "..."],
          "scenarios": ["...", "...", "..."]
        }
        
        注意：
        - 不要反问用户，直接基于输入内容生成结果。
        - 完成分析后，必须调用 doTerminate 工具结束任务。
        - 确保输出的 JSON 格式正确且完整，以便后续智能体能够正确解析。
        """);

        this.setNextStepPrompt("""
        根据当前任务进度，决定下一步操作。
        如果需要外部数据，选择合适的工具。
        **如果已经可以输出最终结果，请在本轮中同时输出 JSON 并调用 doTerminate 工具。**
        每次使用工具后，说明结果并规划下一步。
        若想在任何时候停止，调用 doTerminate。
        """);

//        this.setChatClient(ChatClient.builder(dashscopeChatModel).build());

        // 🔥 注册
//        registry.register("analysis", "商品分析", this);
//        registry.register("analysis", "商品分析", () -> new AnalysisAgent(registry, tools, dashscopeChatModel));
}
}

package com.iverson.aiagent.agent.impl;

import com.iverson.aiagent.agent.base.ToolCallAgent;
import org.springframework.ai.tool.ToolCallback;


public class CopywritingAgent extends ToolCallAgent {

    public CopywritingAgent(ToolCallback[] tools) {

        super(tools);

        this.setName("copywriting");

        this.setSystemPrompt("""
        你是一个电商文案专家。你的输入是商品分析结果（JSON 格式），包含目标用户、卖点、使用场景。
        
        请基于输入生成以下三部分文案，输出 JSON 格式：
        1. 商品标题（20 字以内，吸引眼球）
        2. 商品描述（150 字左右，突出核心价值）
        3. 卖点文案（3 条，每条 30 字以内，适合详情页或短视频脚本）
        
        输出格式示例：
        {
          "title": "...",
          "description": "...",
          "sellingPoints": ["...", "...", "..."]
        }
        
        注意：
        - 不要反问用户，直接根据输入生成文案。
        - 完成文案生成后，必须调用 doTerminate 工具结束任务。
        - 确保输出的 JSON 格式正确且完整，以便后续智能体能够正确解析。
        - 如果你收到的输入不是完整的 JSON 格式，请尝试从输入中提取商品分析信息，然后生成文案。
        """);

        this.setNextStepPrompt("""
        根据当前任务进度，决定下一步操作。
        如果需要外部数据，选择合适的工具。
        **如果已经可以输出最终结果，请在本轮中同时输出 JSON 并调用 doTerminate 工具。**
        每次使用工具后，说明结果并规划下一步。
        若想在任何时候停止，调用 doTerminate。
        """);

//        this.setChatClient(ChatClient.builder(dashscopeChatModel).build());

//        registry.register("copywriting", "文案生成", this);
//        registry.register("copywriting", "文案生成", () -> new CopywritingAgent(registry, tools, dashscopeChatModel));

    }
}
package com.iverson.aiagent.agent.impl;

import com.iverson.aiagent.agent.base.ToolCallAgent;
import org.springframework.ai.tool.ToolCallback;


public class SeoAgent extends ToolCallAgent {

    public SeoAgent(ToolCallback[] tools) {

        super(tools);

        this.setName("seo");

        this.setSystemPrompt("""
        你是一个 SEO 专家。你的输入是商品文案（JSON 格式），包含标题、描述、卖点文案。
        
        请进行以下优化，输出 JSON 格式：
        1. 提取 5-8 个核心关键词（基于商品卖点和用户搜索习惯）
        2. 优化标题（在原标题基础上融入 1-2 个关键词，长度 25 字以内）
        3. 优化描述（在原文案基础上自然嵌入关键词，保持可读性，180 字以内）
        
        输出格式示例：
        {
          "keywords": ["关键词1", "关键词2", ...],
          "optimizedTitle": "...",
          "optimizedDescription": "..."
        }
        
        注意：
        - 不要反问用户，直接基于输入内容优化。
        - 完成优化后，必须调用 doTerminate 工具结束任务。
        - 确保输出的 JSON 格式正确且完整。
        - 如果你收到的输入不是完整的 JSON 格式，请尝试从输入中提取商品文案信息，然后进行 SEO 优化。
        - 你可以使用 searchWeb 工具获取更多相关信息，但优先使用前一个智能体提供的信息。
        """);

        this.setNextStepPrompt("""
        根据当前任务进度，决定下一步操作。
        如果需要外部数据，选择合适的工具。
        **如果已经可以输出最终结果，请在本轮中同时输出 JSON 并调用 doTerminate 工具。**
        每次使用工具后，说明结果并规划下一步。
        若想在任何时候停止，调用 doTerminate。
        """);

//        this.setChatClient(ChatClient.builder(dashscopeChatModel).build());

//        registry.register("seo", "SEO优化", this);
//        registry.register("seo", "SEO优化", () -> new SeoAgent(registry, tools, dashscopeChatModel));

    }
}

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

package com.iverson.aiagent.agent.model;

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

package com.iverson.aiagent.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class ToolRegistration {


    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool
        );
    }
}


