package com.iverson.aiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于数据库持久化的对话记忆实现，使用 Kryo 序列化消息列表并存入 BLOB 字段。
 */
@Slf4j
public class DatabaseChatMemoryKryo implements ChatMemory {

    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造方法，自动初始化数据库表（若不存在）。
     * @param dataSource 数据源
     */
    public DatabaseChatMemoryKryo(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        initTable();
    }

    /**
     * 初始化数据库表：如果表不存在则创建。
     * 表结构：conversation_id VARCHAR(255) PRIMARY KEY, messages BLOB
     */
    private void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS chat_memory (" +
                     "conversation_id VARCHAR(255) PRIMARY KEY, " +
                     "messages BLOB)";
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
        String sql = "DELETE FROM chat_memory WHERE conversation_id = ?";
        jdbcTemplate.update(sql, conversationId);
    }

    /**
     * 从数据库获取指定对话的所有消息。
     * @param conversationId 对话ID
     * @return 消息列表（若不存在则返回空列表）
     */
    @SuppressWarnings("unchecked")
//    private List<Message> getConversationMessages(String conversationId) {
//        String sql = "SELECT messages FROM chat_memory WHERE conversation_id = ?";
//        return jdbcTemplate.query(sql, rs -> {
//            if (!rs.next()) {
//                return new ArrayList<>();
//            }
//            byte[] bytes = rs.getBytes("messages");
//            if (bytes == null || bytes.length == 0) {
//                return new ArrayList<>();
//            }
//            try (Input input = new Input(new ByteArrayInputStream(bytes))) {
//                return kryo.readObject(input, ArrayList.class);
//            } catch (Exception e) {
//                // 可替换为日志记录
//                e.printStackTrace();
//                return new ArrayList<>();
//            }
//        }, conversationId);
//    }
    // 在类中添加日志
//    private static final Logger logger = LoggerFactory.getLogger(DatabaseChatMemory.class);

    private List<Message> getConversationMessages(String conversationId) {
        String sql = "SELECT messages FROM chat_memory WHERE conversation_id = ?";
        try {
            return jdbcTemplate.query(sql, this::extractMessages, conversationId);
        } catch (DataAccessException e) {
            log.error("Database error while fetching messages for conversation: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 从 ResultSet 中提取消息列表（最多一行）。
     */
    private List<Message> extractMessages(ResultSet rs) throws SQLException {
        if (!rs.next()) {
            // 没有找到该对话的记录
            log.debug("No messages found for conversation, returning empty list.");
            return new ArrayList<>();
        }

        byte[] messagesBytes = rs.getBytes("messages");
        if (messagesBytes == null || messagesBytes.length == 0) {
            log.debug("Messages BLOB is empty for conversation, returning empty list.");
            return new ArrayList<>();
        }

        return deserializeMessages(messagesBytes);
    }

    /**
     * 使用 Kryo 将字节数组反序列化为 List<Message>。
     */
    @SuppressWarnings("unchecked")
    private List<Message> deserializeMessages(byte[] bytes) {
        try (Input input = new Input(new ByteArrayInputStream(bytes))) {
            List<Message> messages = kryo.readObject(input, ArrayList.class);
            log.info("+++++++++++++++++++++++++++++++++++++++++++++++++");
            log.info("Message deserialization completed. messages: {}", messages);
            log.debug("Successfully deserialized {} messages.", messages.size());
            return messages;
        } catch (Exception e) {
            log.error("Failed to deserialize messages from bytes, returning empty list.", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存或更新对话的消息列表到数据库。
     * @param conversationId 对话ID
     * @param messages 消息列表
     */
    private void saveConversation(String conversationId, List<Message> messages) {
        byte[] bytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Output output = new Output(baos)) {
            kryo.writeObject(output, messages);
            output.flush();
            bytes = baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // 使用 MERGE 或 INSERT ... ON DUPLICATE KEY UPDATE 语法（根据数据库调整）
        // 此处使用通用的判断是否存在后更新或插入，保证数据库兼容性
        boolean exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM chat_memory WHERE conversation_id = ?",
                Integer.class, conversationId) > 0;

        if (exists) {
            jdbcTemplate.update("UPDATE chat_memory SET messages = ? WHERE conversation_id = ?",
                    bytes, conversationId);
        } else {
            jdbcTemplate.update("INSERT INTO chat_memory (conversation_id, messages) VALUES (?, ?)",
                    conversationId, bytes);
        }
    }
}