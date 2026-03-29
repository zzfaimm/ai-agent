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

    @Override
    public String step() {
        // 此方法不会被调用，因为我们重写了doRun方法
        return "ToolCallAgent不需要执行step方法";
    }
}
