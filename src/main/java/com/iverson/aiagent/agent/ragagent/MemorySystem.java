package com.iverson.aiagent.agent.ragagent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 简单的记忆系统，用于存储对话历史
 */
public class MemorySystem {
    
    private final LinkedBlockingDeque<String> history;
    private final int maxSize;
    
    public MemorySystem(int maxSize) {
        this.history = new LinkedBlockingDeque<>(maxSize);
        this.maxSize = maxSize;
    }
    
    public MemorySystem() {
        this(10); // 默认存储10条记录
    }
    
    /**
     * 添加对话记录
     */
    public void add(String content) {
        if (history.size() >= maxSize) {
            history.pollFirst();
        }
        history.offerLast(content);
    }
    
    /**
     * 获取对话历史
     */
    public List<String> getHistory() {
        return new ArrayList<>(history);
    }
    
    /**
     * 清空历史
     */
    public void clear() {
        history.clear();
    }
    
    /**
     * 获取历史摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        for (String item : history) {
            sb.append(item).append("\n");
        }
        return sb.toString();
    }
}
