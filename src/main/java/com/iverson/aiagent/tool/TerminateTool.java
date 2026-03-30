package com.iverson.aiagent.tool;

import org.springframework.ai.tool.annotation.Tool;
import java.util.Map;

public class TerminateTool {

    @Tool(description = "结束当前对话轮次。当完成用户请求或需要结束本轮交互时调用。")
    public String doTerminate(Map<String, Object> arguments) {
        return "任务结束";
    }
}