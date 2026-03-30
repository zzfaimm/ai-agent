package com.iverson.aiagent.agent.simpleagent.base;

import com.iverson.aiagent.agent.manyagent.model.SharedState;
import com.iverson.aiagent.agent.simpleagent.model.SimpleAgentState;
import com.iverson.aiagent.chatmemory.DatabaseChatMemory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.util.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
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
public abstract class SimpleBaseAgent {

    // 核心属性
    private String name;

    // 提示
    private String systemPrompt;
    private String nextStepPrompt;

    // 多Agent之间的数据通信
    protected SharedState sharedState;

    // 状态
    private SimpleAgentState state = SimpleAgentState.IDLE;

    // 执行控制
    private int maxSteps = 10;
    private int currentStep = 0;

    // LLM
    private ChatClient chatClient;

    // Memory(需要自主维护上下文)
    private List<Message> messageList = new ArrayList<>();
    
    // 重要消息列表，用于持久化存储
    private List<Message> importantMessageList = new ArrayList<>();
    
    // 对话记忆
    private ChatMemory chatMemory;
    private String conversationId;

    /**
     * 初始化对话记忆
     */
    public void initChatMemory(DatabaseChatMemory chatMemory, String conversationId) {
        this.chatMemory = chatMemory;
        this.conversationId = conversationId;
        // 加载历史消息
        if (conversationId != null) {
            importantMessageList = chatMemory.get(conversationId, 100);
            // 将重要消息加载到上下文
            messageList.addAll(importantMessageList);
            log.info("{}: 已加载历史重要消息 {} 条", getName(), importantMessageList.size());
        }
    }

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
                if (this.state != SimpleAgentState.IDLE) {
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
                state = SimpleAgentState.RUNNING;
                log.info("{}: 状态变更为: {}", getName(), state);
                // 记录消息上下文
                UserMessage userMessage = new UserMessage(userPrompt);
                messageList.add(userMessage);
                // 添加到重要消息列表
                importantMessageList.add(userMessage);
                log.info("{}: 已添加用户消息到上下文和重要消息列表", getName());

                try {
                    for (int i = 0; i < maxSteps && state != SimpleAgentState.FINISHED; i++) {
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
                        state = SimpleAgentState.FINISHED;
                        log.info("{}: 达到最大步骤限制，执行结束", getName());
                        emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                    } else {
                        log.info("{}: 任务正常完成", getName());
                    }
                    // 正常完成
                    emitter.complete();
                } catch (Exception e) {
                    state = SimpleAgentState.ERROR;
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
            this.state = SimpleAgentState.ERROR;
            this.cleanup();
            log.warn("{}: SSE连接超时", getName());
        });

        emitter.onCompletion(() -> {
            if (this.state == SimpleAgentState.RUNNING) {
                this.state = SimpleAgentState.FINISHED;
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
        if (this.state != SimpleAgentState.IDLE){
            log.warn("{}: 无法从状态运行代理: {}", getName(), this.state);
            throw new RuntimeException("cannot run agent from state " + this.state);
        }
        if (StringUtil.isEmpty(userPrompt)){
            log.warn("{}: 空提示词", getName());
            throw new RuntimeException("userPrompt is empty");
        }
        //更改状态
        state = SimpleAgentState.RUNNING;
        log.info("{}: 状态变更为: {}", getName(), state);
        //记录消息上下文
        UserMessage userMessage = new UserMessage(userPrompt);
        messageList.add(userMessage);
        log.info("{}: 已添加用户消息到上下文", getName());
        //保存结果列表
        List<String> results = new ArrayList<>();

        try {
            for (int i = 0; i < maxSteps && state != SimpleAgentState.FINISHED; i++){
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
                state = SimpleAgentState.FINISHED;
                log.info("{}: 达到最大步骤限制，执行结束", getName());
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            } else {
                log.info("{}: 任务正常完成", getName());
            }
            String finalResult = String.join("\n", results);
            log.info("{}: 执行结果: {}", getName(), finalResult);
            return finalResult;
        } catch (Exception e) {
            state = SimpleAgentState.ERROR;
            log.error("{}: 执行智能体失败", getName(), e);
            return "error" + e.getMessage();
        } finally {
            //清理资源
            log.info("{}: 开始清理资源", getName());
            this.cleanup();
            log.info("{}: 资源清理完成", getName());
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
        // 保存对话记忆
        if (chatMemory != null && conversationId != null && !importantMessageList.isEmpty()) {
            chatMemory.add(conversationId, importantMessageList);
            log.info("{}: 已保存对话记忆，重要消息数: {}", getName(), importantMessageList.size());
        }
    }
    
    /**
     * 添加助手的最终回复到重要消息列表
     */
    public void addAssistantMessageToImportantList(Message message) {
        importantMessageList.add(message);
        log.info("{}: 已添加助手消息到重要消息列表", getName());
    }

}
