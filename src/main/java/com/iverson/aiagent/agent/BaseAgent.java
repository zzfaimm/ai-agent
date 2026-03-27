package com.iverson.aiagent.agent;

import com.iverson.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.util.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

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
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt){
        // 1.做校验
        if (this.state != AgentState.IDLE){
            throw new RuntimeException("cannot run agent from state " + this.state);
        }
        if (StringUtil.isEmpty(userPrompt)){
            throw new RuntimeException("userPrompt is empty");
        }
        //更改状态
        state = AgentState.RUNNING;
        //记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        //保存结果列表
        List<String> results = new ArrayList<>();

        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++){
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("executing step " + stepNumber + " of " + maxSteps);
                //单步执行
                String stepResult = step();
                String result = "step " + stepNumber + " of " + stepResult;
                results.add(result);
            }
            //检查是否超出步骤限制
            if (currentStep >= maxSteps){
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "error" + e.getMessage();
        } finally {
            //清理资源
            this.cleanup();
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

    }


























}
