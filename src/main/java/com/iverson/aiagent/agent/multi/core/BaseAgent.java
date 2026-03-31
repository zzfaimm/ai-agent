package com.iverson.aiagent.agent.multi.core;

import com.iverson.aiagent.agent.multi.model.AgentResult;
import com.iverson.aiagent.agent.multi.model.AgentState;
import com.iverson.aiagent.agent.multi.model.SharedState;
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
    protected AgentState state = AgentState.IDLE;

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


    public AgentResult run(SharedState sharedState) {
        log.info("{}: 开始执行，共享状态: {}", getName(), sharedState);

        try {
            return doRun(sharedState);
        } catch (Exception e) {
            log.error("{}: 执行异常", getName(), e);
            AgentResult result = new AgentResult();
            result.setSuccess(false);
            result.setActionType(AgentResult.ActionType.RETRY);
            return result;
        }
    }

    protected abstract AgentResult doRun(SharedState sharedState);


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
