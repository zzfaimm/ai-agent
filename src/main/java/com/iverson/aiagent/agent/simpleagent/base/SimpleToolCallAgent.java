package com.iverson.aiagent.agent.simpleagent.base;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import com.iverson.aiagent.agent.simpleagent.model.SimpleAgentState;
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
public class SimpleToolCallAgent extends SimpleReActAgent {

    //可用的工具
    private final ToolCallback[] availableTools;

    //可用的工具
    private String chatModelResult = null;

    // 保存工具调用响应信息
    private ChatResponse toolCallChatResponse;

    // 工具调用的管理者
    private final ToolCallingManager toolCallingManager;

    // 禁止内置的工具调用机制运行，自己维护上下文
    private final ChatOptions chatOptions;

    public SimpleToolCallAgent(ToolCallback[] availableTools){
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁止 Spring AI 内置的工具调用机制，自己维护选项和上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true)
                .build();
    }

    /**
     * 处理当前状态并且决定下一步行动
     * @param
     * @return 是否需要执行工具
     */
    @Override
    public boolean think() {
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        List<Message> messageList =  getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);

        try {
            // 获取带工具选项的响应
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应， 用于 Act
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 输出提示信息
            chatModelResult = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            log.info(getName() + "的思考" + chatModelResult);
            log.info(getName() + "选择了" + toolCallList.size() + "个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s, 参数：%s",
                            toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            if (toolCallList.isEmpty()) {

                // 只有不调用工具时，才需要记录助手消息
                getMessageList().add(assistantMessage);
                // 添加到重要消息列表
                addAssistantMessageToImportantList(assistantMessage);
                // 🔥 核心修改：直接结束
                setState(SimpleAgentState.FINISHED);
                return false;
            }else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录

                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题:" + e.getMessage());
            getMessageList().add(new AssistantMessage("think方法时遇到错误" + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并且处理结果
     * @param
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()){
            return "没有工具调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory方法已经包含了助手消息和工具调用返回的结果
        List<Message> conversationHistory = toolExecutionResult.conversationHistory();
        setMessageList(conversationHistory);
        
        // 查找助手消息并添加到重要消息列表
        for (Message message : conversationHistory) {
            if (message instanceof AssistantMessage) {
                AssistantMessage assistantMessage = (AssistantMessage) message;
                // 只添加带有终止工具调用的助手消息
                if (assistantMessage.getToolCalls() != null && !assistantMessage.getToolCalls().isEmpty()) {
                    boolean hasTerminateTool = assistantMessage.getToolCalls().stream()
                            .anyMatch(toolCall -> "terminate".equals(toolCall.name()));
                    if (hasTerminateTool) {
                        addAssistantMessageToImportantList(assistantMessage);
                        break;
                    }
                }
            }
        }
        
        // 当前工具调用的结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(conversationHistory);
        String result = toolResponseMessage.getResponses().stream()
                .map(response -> "工具" + response.name() + "完成了他的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n"));
        // 判断是否调用了终止工具
        boolean doTerminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        if (doTerminateToolCalled) {
            setState(SimpleAgentState.FINISHED);
        }
        log.info(result);
        return chatModelResult;
    }
}
