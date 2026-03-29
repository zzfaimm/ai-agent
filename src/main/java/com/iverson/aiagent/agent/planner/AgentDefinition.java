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