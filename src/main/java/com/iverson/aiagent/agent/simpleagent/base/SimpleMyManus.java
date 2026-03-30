package com.iverson.aiagent.agent.simpleagent.base;

import com.iverson.aiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * @ClassName: Manus
 * @Description: TODO(描述这个类的作用)
 * @Author: zhuze
 * @Date: 3/26/2026 10:19 PM
 * @Version: 1.0
 */
@Component
public class SimpleMyManus extends SimpleToolCallAgent {

    public SimpleMyManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("MyManus");
        String SYSTEM_PROMPT = """
                你是 小猪，一个全能型 AI 助手，目标是解决用户提出的任何任务。
                你拥有多种工具，可以根据需要调用它们，高效地完成复杂的请求。
                
                **语言要求：**
                请使用与用户相同的语言进行回复，除非用户明确要求使用其他语言。
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                你是一个可以调用工具的 AI 助手，运行在 ReAct 循环中。
                
                **循环流程：**
                - 用户发送消息。
                - 你可以用文字回复，并可以选择调用工具（可多次调用）。
                - 当你处理完当前用户消息，并给出了最终回复后（无论是否调用了工具），**必须**在同一个响应中调用 `doTerminate` 工具，以结束本轮。
                - 系统收到 `doTerminate` 后会等待用户的下一轮消息。不要认为所有事情必须在一轮内解决。
                
                **调用 `doTerminate` 的规则：**
                1. 只要你要输出针对当前用户消息的最终回答（即使是简单的问候或要求澄清），你**必须**在同一个响应中包含一个 `doTerminate` 工具调用。
                2. 不要只输出文本而不调用 `doTerminate`。当你本轮回应完成时，必须调用 `doTerminate`。
                3. `doTerminate` 工具不需要参数，使用 `"arguments": {}`（空对象）。
                4. **这是最重要的规则：如果不调用 `doTerminate` 工具，会话将无法结束，你会被要求重复相同的内容，陷入无限循环。**
                
                **语言要求：**
                - 请使用与用户相同的语言进行回复，除非用户明确要求使用其他语言。
                
                **示例 1：简单问候**
                用户：你好
                助手：{
                  "content": "你好！今天有什么可以帮你的？",
                  "tool_calls": [{"name": "doTerminate", "arguments": {}}]
                }
                
                **示例 2：使用工具后结束**
                用户：东京天气怎么样？
                助手：{
                  "content": "我来查一下。",
                  "tool_calls": [{"name": "weather", "arguments": {"city":"东京"}}]
                }
                -- 系统返回天气结果 --
                助手：{
                  "content": "东京天气晴朗，25°C。",
                  "tool_calls": [{"name": "doTerminate", "arguments": {}}]
                }
                
                **示例 3：要求澄清**
                用户：介绍一下 Spring AI。
                助手：{
                  "content": "Spring AI 有很多方面。你想了解函数调用、向量存储，还是其他内容？",
                  "tool_calls": [{"name": "doTerminate", "arguments": {}}]
                }
                -- 用户回答后开始新的一轮 --
                
                **错误示例（会导致无限循环）：**
                用户：你好
                助手：你好！今天有什么可以帮你的？
                （错误原因：没有调用 doTerminate 工具）
                
                **重要：**
                - 不要在轮次之间保留状态；每一轮都从用户的新消息开始。
                - 每次回应结束时必须调用 `doTerminate`，这是结束本轮的强制要求。
                - **忘记调用 `doTerminate` 工具是最严重的错误，会导致会话无法正常结束。**
                - 无论你的回答多么完美，只要没有调用 `doTerminate` 工具，就是失败的响应。
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}