package com.iverson.aiagent.chatmemory;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.iverson.aiagent.chatmemory.entity.ChatMemoryEntity;
import com.iverson.aiagent.chatmemory.mapper.ChatMemoryMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于数据库持久化的对话记忆实现，使用 JSON 格式存储消息列表。
 * 通过将消息转换为Map进行存储，避免Jackson多态序列化问题。
 */
public class DatabaseChatMemory implements ChatMemory {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseChatMemory.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChatMemoryMapper chatMemoryMapper;

    public DatabaseChatMemory(ChatMemoryMapper chatMemoryMapper) {
        this.chatMemoryMapper = chatMemoryMapper;
        initTable();
    }

    /**
     * 初始化数据库表：如果表不存在则创建。
     * 表结构：conversation_id VARCHAR(255) PRIMARY KEY, messages JSON, created_at DATETIME, updated_at DATETIME
     */
    public void initTable() {
        // MyBatis Plus会自动处理表结构，这里可以留空或添加自定义初始化逻辑
        logger.info("DatabaseChatMemory initialized");
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> conversationMessages = getConversationMessages(conversationId);
        conversationMessages.addAll(messages);
        saveConversation(conversationId, conversationMessages);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> allMessages = getConversationMessages(conversationId);
        if (allMessages.size() <= lastN) {
            return allMessages;
        }
        return allMessages.subList(allMessages.size() - lastN, allMessages.size());
    }

    @Override
    public void clear(String conversationId) {
        QueryWrapper<ChatMemoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        chatMemoryMapper.delete(queryWrapper);
    }

    /**
     * 从数据库获取指定对话的所有消息。
     * @param conversationId 对话ID
     * @return 消息列表（若不存在则返回空列表）
     */
    private List<Message> getConversationMessages(String conversationId) {
        try {
            QueryWrapper<ChatMemoryEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("conversation_id", conversationId);
            ChatMemoryEntity entity = chatMemoryMapper.selectOne(queryWrapper);
            
            if (entity == null) {
                return new ArrayList<>();
            }
            
            String json = entity.getMessages();
            if (json == null || json.isEmpty()) {
                return new ArrayList<>();
            }
            
            return deserializeMessages(json);
        } catch (Exception e) {
            logger.error("Failed to fetch messages for conversation: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 将 JSON 字符串反序列化为 List<Message>。
     */
    private List<Message> deserializeMessages(String json) {
        try {
            List<Map<String, Object>> messageMaps = OBJECT_MAPPER.readValue(json, new ArrayList<Map<String, Object>>().getClass());
            List<Message> messages = new ArrayList<>();
            for (Map<String, Object> map : messageMaps) {
                String className = (String) map.get("@class");
                String text = (String) map.get("text");
                
                if (className != null && text != null) {
                    if (className.contains("UserMessage")) {
                        messages.add(new UserMessage(text));
                    } else if (className.contains("AssistantMessage")) {
                        messages.add(new AssistantMessage(text));
                    } else if (className.contains("SystemMessage")) {
                        messages.add(new SystemMessage(text));
                    }
                }
            }
            return messages;
        } catch (Exception e) {
            logger.error("Failed to deserialize messages JSON: {}", json, e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存或更新对话的消息列表到数据库。
     * @param conversationId 对话ID
     * @param messages 消息列表
     */
    private void saveConversation(String conversationId, List<Message> messages) {
        String json = serializeMessages(messages);
        if (json == null) {
            return;
        }

        QueryWrapper<ChatMemoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        ChatMemoryEntity entity = chatMemoryMapper.selectOne(queryWrapper);

        LocalDateTime now = LocalDateTime.now();
        if (entity != null) {
            // 更新现有记录
            entity.setMessages(json);
            entity.setUpdatedAt(now);
            chatMemoryMapper.updateById(entity);
        } else {
            // 创建新记录
            entity = new ChatMemoryEntity();
            entity.setConversationId(conversationId);
            entity.setMessages(json);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            chatMemoryMapper.insert(entity);
        }
    }

    /**
     * 将 List<Message> 序列化为 JSON 字符串。
     */
    private String serializeMessages(List<Message> messages) {
        try {
            List<Map<String, Object>> messageMaps = new ArrayList<>();
            for (Message message : messages) {
                Map<String, Object> map = new HashMap<>();
                map.put("@class", message.getClass().getName());
                map.put("text", message.getText());
                messageMaps.add(map);
            }
            String json = OBJECT_MAPPER.writeValueAsString(messageMaps);
            logger.info("Serialized JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize messages", e);
            return null;
        }
    }
}