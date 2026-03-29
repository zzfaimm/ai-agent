package com.iverson.aiagent.agent.planner;

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
}