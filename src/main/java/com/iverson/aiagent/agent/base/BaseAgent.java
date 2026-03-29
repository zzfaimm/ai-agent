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
