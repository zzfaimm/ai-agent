package com.iverson.aiagent.chatmemory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
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

    private final JdbcTemplate jdbcTemplate;

    public DatabaseChatMemory(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initTable();
    }


    /**
     * 初始化数据库表：如果表不存在则创建。
     * 表结构：conversation_id VARCHAR(255) PRIMARY KEY, messages JSON
     */
    private void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS chat_memory_json (" +
                "conversation_id VARCHAR(255) PRIMARY KEY, " +
                "messages JSON)";
        jdbcTemplate.execute(sql);
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
        String sql = "DELETE FROM chat_memory_json WHERE conversation_id = ?";
        jdbcTemplate.update(sql, conversationId);
    }

    /**
     * 从数据库获取指定对话的所有消息。
     * @param conversationId 对话ID
     * @return 消息列表（若不存在则返回空列表）
     */
    private List<Message> getConversationMessages(String conversationId) {
        String sql = "SELECT messages FROM chat_memory_json WHERE conversation_id = ?";
        try {
            return jdbcTemplate.query(sql, rs -> {
                if (!rs.next()) {
                    return new ArrayList<>();
                }
                String json = rs.getString("messages");
                if (json == null || json.isEmpty()) {
                    return new ArrayList<>();
                }
                return deserializeMessages(json);
            }, conversationId);
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

        boolean exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_memory_json WHERE conversation_id = ?",
                Integer.class, conversationId) > 0;

        if (exists) {
            jdbcTemplate.update("UPDATE chat_memory_json SET messages = ? WHERE conversation_id = ?",
                    json, conversationId);
        } else {
            jdbcTemplate.update("INSERT INTO chat_memory_json (conversation_id, messages) VALUES (?, ?)",
                    conversationId, json);
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