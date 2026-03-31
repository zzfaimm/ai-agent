package com.iverson.aiagent.agent.multi.planner;

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

    public Task() {
        // 无参构造函数，用于Jackson反序列化
    }
}