package com.iverson.aiagent.chatmemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对话记忆实体类
 */
@Data
@TableName("chat_memory_json")
public class ChatMemoryEntity {
    
    @TableId(type = IdType.INPUT)
    private String conversationId;
    
    private String messages;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
