package com.iverson.aiagent.rag.ragagent;

/**
 * 工具接口，定义工具的基本方法
 */
public interface Tool {
    /**
     * 获取工具名称
     */
    String getName();
    
    /**
     * 获取工具描述
     */
    String getDescription();
    
    /**
     * 执行工具
     */
    String execute(String input);
}
