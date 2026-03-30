package com.iverson.aiagent.agent.manyagent.planner;

import lombok.Data;

import java.util.List;

@Data
public class Plan {
    private List<Task> tasks;  // Task 包含 name 和 dependsOn
    // 可选：并行执行的分组
    private List<List<String>> parallelGroups;

    public Plan(List<Task> tasks) {
        this.tasks = tasks;
    }

    public Plan() {
        // 无参构造函数，用于Jackson反序列化
    }
}