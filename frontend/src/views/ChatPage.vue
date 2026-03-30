<template>
  <div class="chat-page">
    <div class="chat-container">
      <!-- 左侧历史会话列表 -->
      <div class="sidebar">
        <div class="sidebar-header">
          <h3>历史会话</h3>
        </div>
        <div class="session-list">
          <div 
            v-for="(session, index) in currentSessions" 
            :key="session.id"
            class="session-item"
            :class="{ active: currentSessionId === session.id }"
            @click="switchSession(session.id)"
          >
            <div class="session-title">{{ session.title }}</div>
            <div class="session-time">{{ formatTime(session.updatedAt) }}</div>
          </div>
          <div class="session-item new-session" @click="createNewSession">
            <el-icon><svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2Z"/></svg></el-icon>
            <span>新建会话</span>
          </div>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="chat-main">
        <div class="chat-header">
          <div class="header-content">
            <h2>AI 聊天助手</h2>
            <div class="header-actions">
              <el-button circle icon="Setting"></el-button>
              <el-button circle icon="User"></el-button>
            </div>
          </div>
          <div class="function-tabs">
            <el-tabs v-model="currentAgent" @tab-click="handleAgentChange">
              <el-tab-pane label="恋爱机器人" name="loveApp">
                <template #label>
                  <div class="tab-label">
                    <el-icon class="tab-icon"><svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path fill="currentColor" d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5C2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></el-icon>
                    <span>恋爱机器人</span>
                  </div>
                </template>
              </el-tab-pane>
              <el-tab-pane label="超级智能体" name="manus">
                <template #label>
                  <div class="tab-label">
                    <el-icon class="tab-icon"><svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg></el-icon>
                    <span>超级智能体</span>
                  </div>
                </template>
              </el-tab-pane>
              <el-tab-pane label="多Agent" name="agent">
                <template #label>
                  <div class="tab-label">
                    <el-icon class="tab-icon"><svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg></el-icon>
                    <span>多Agent</span>
                  </div>
                </template>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
        <div class="chat-content">
          <ChatWindow 
            :messageList="sessionMessages" 
            :loading="loading"
          />
        </div>
        <div class="chat-input">
          <InputBox 
            :loading="loading"
            @send="handleSendMessage"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import ChatWindow from '../components/ChatWindow.vue';
import InputBox from '../components/InputBox.vue';
import { loveAppChat, manusChat, generateAgent } from '../api/ai';
import { useChatStore } from '../stores/chat';

// 状态管理
const chatStore = useChatStore();
const loading = ref(false);
const currentSessionId = ref('');

// 计算属性
const currentAgent = computed({
  get: () => chatStore.activeAgent,
  set: (value) => chatStore.setActiveAgent(value)
});

const currentSessions = computed(() => {
  return chatStore.getCurrentSessions();
});

const sessionMessages = computed(() => {
  return chatStore.getSessionMessages(currentAgent.value, currentSessionId.value);
});

// 格式化时间
const formatTime = (isoString) => {
  const date = new Date(isoString);
  return date.toLocaleString();
};

// 生成唯一消息ID
const generateMessageId = () => {
  return Date.now().toString(36) + Math.random().toString(36).substr(2);
};

// 处理发送消息
const handleSendMessage = (message) => {
  // 如果没有当前会话，创建一个
  if (!currentSessionId.value) {
    currentSessionId.value = chatStore.createSession(currentAgent.value);
  }

  // 添加用户消息
  const userMessage = {
    id: generateMessageId(),
    role: 'user',
    content: message
  };
  chatStore.addMessage(currentAgent.value, currentSessionId.value, userMessage);

  // 更新会话标题（如果是新会话）
  const currentSession = currentSessions.value.find(s => s.id === currentSessionId.value);
  if (currentSession && currentSession.title === '新会话') {
    const newTitle = message.substring(0, 20) + (message.length > 20 ? '...' : '');
    chatStore.updateSessionTitle(currentAgent.value, currentSessionId.value, newTitle);
  }

  // 创建空的AI消息
  const aiMessageId = generateMessageId();
  const aiMessage = {
    id: aiMessageId,
    role: 'ai',
    content: ''
  };
  chatStore.addMessage(currentAgent.value, currentSessionId.value, aiMessage);

  // 设置加载状态
  loading.value = true;

  // 跟踪是否已经收到了有效的消息
  let hasReceivedMessage = false;

  // 根据当前选择的智能体调用不同的API
  if (currentAgent.value === 'loveApp') {
    // 调用恋爱机器人 SSE 接口
    const eventSource = loveAppChat(
      message,
      currentSessionId.value,
      (data) => {
        // 标记已经收到了消息
        hasReceivedMessage = true;
        // 逐步填充AI消息
        const currentMessages = chatStore.getSessionMessages(currentAgent.value, currentSessionId.value);
        const aiMessage = currentMessages.find(m => m.id === aiMessageId);
        if (aiMessage) {
          chatStore.updateAiMessage(currentAgent.value, currentSessionId.value, aiMessageId, aiMessage.content + data);
        }
      },
      (error) => {
        console.error('聊天错误:', error);
        // 只有在没有收到任何消息的情况下才显示错误提示
        if (!hasReceivedMessage) {
          const currentMessages = chatStore.getSessionMessages(currentAgent.value, currentSessionId.value);
          const aiMessage = currentMessages.find(m => m.id === aiMessageId);
          if (aiMessage) {
            const existingContent = aiMessage.content || '';
            chatStore.updateAiMessage(currentAgent.value, currentSessionId.value, aiMessageId, existingContent + '\n\n[聊天失败，请重试]');
          }
        }
        loading.value = false;
      },
      () => {
        // 完成时关闭连接
        loading.value = false;
      }
    );

    // 保存 eventSource 以便后续关闭
    window.eventSource = eventSource;
  } else if (currentAgent.value === 'manus') {
        // 调用超级智能体 SSE 接口
        const eventSource = manusChat(
          message,
          currentSessionId.value,
          (data) => {
            // 标记已经收到了消息
            hasReceivedMessage = true;
            // 逐步填充AI消息
            const currentMessages = chatStore.getSessionMessages(currentAgent.value, currentSessionId.value);
            const aiMessage = currentMessages.find(m => m.id === aiMessageId);
            if (aiMessage) {
              chatStore.updateAiMessage(currentAgent.value, currentSessionId.value, aiMessageId, aiMessage.content + data);
            }
          },
          (error) => {
            console.error('聊天错误:', error);
            // 只有在没有收到任何消息的情况下才显示错误提示
            if (!hasReceivedMessage) {
              const currentMessages = chatStore.getSessionMessages(currentAgent.value, currentSessionId.value);
              const aiMessage = currentMessages.find(m => m.id === aiMessageId);
              if (aiMessage) {
                const existingContent = aiMessage.content || '';
                chatStore.updateAiMessage(currentAgent.value, currentSessionId.value, aiMessageId, existingContent + '\n\n[聊天失败，请重试]');
              }
            }
            loading.value = false;
          },
          () => {
            // 完成时关闭连接
            loading.value = false;
          }
        );

    // 保存 eventSource 以便后续关闭
    window.eventSource = eventSource;
  } else if (currentAgent.value === 'agent') {
    // 调用多Agent 普通接口
    generateAgent(message)
      .then((response) => {
        // 填充AI消息
        chatStore.updateAiMessage(currentAgent.value, currentSessionId.value, aiMessageId, response.result || response.content || '');
        loading.value = false;
      })
      .catch((error) => {
        console.error('生成失败:', error);
        // 保留已接收的内容，仅在末尾添加错误提示
        const currentMessages = chatStore.getSessionMessages(currentAgent.value, currentSessionId.value);
        const aiMessage = currentMessages.find(m => m.id === aiMessageId);
        if (aiMessage) {
          const existingContent = aiMessage.content || '';
          chatStore.updateAiMessage(currentAgent.value, currentSessionId.value, aiMessageId, existingContent + '\n\n[生成失败，请重试]');
        }
        loading.value = false;
      });
  }
};

// 处理智能体切换
const handleAgentChange = () => {
  // 切换智能体后，选择第一个会话或创建新会话
  const sessions = chatStore.getCurrentSessions();
  if (sessions.length > 0) {
    currentSessionId.value = sessions[0].id;
  } else {
    // 如果没有会话，创建一个新会话
    currentSessionId.value = chatStore.createSession(currentAgent.value);
  }
};

// 切换会话
const switchSession = (sessionId) => {
  currentSessionId.value = sessionId;
};

// 创建新会话
const createNewSession = () => {
  currentSessionId.value = chatStore.createSession(currentAgent.value);
};

// 页面加载时初始化
onMounted(() => {
  // 获取当前智能体的会话列表
  const sessions = chatStore.getCurrentSessions();
  if (sessions.length > 0) {
    currentSessionId.value = sessions[0].id;
  } else {
    // 如果没有会话，创建一个新会话
    currentSessionId.value = chatStore.createSession(currentAgent.value);
  }
});
</script>

<style scoped>
.chat-page {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  margin: 0;
  padding: 0;
}

.chat-container {
  flex: 1;
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background-color: #ffffff;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  border-radius: 0;
  min-height: 0;
}

/* 左侧会话列表 */
.sidebar {
  flex: 0 0 25%;
  max-width: 320px;
  min-width: 240px;
  border-right: 1px solid #e8e8e8;
  background-color: #fafafa;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  overflow-y: auto;
}

.sidebar-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e8e8e8;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333333;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.session-item {
  padding: 16px 20px;
  border-radius: 12px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background-color: #ffffff;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.session-item:hover {
  background-color: #f8f9fa;
  border-color: #e0e0e0;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.session-item.active {
  background-color: #e6f7ff;
  border-color: #1677ff;
  box-shadow: 0 2px 4px rgba(22, 119, 255, 0.15);
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: #333333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.session-time {
  font-size: 12px;
  color: #999999;
  line-height: 1.2;
}

.new-session {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1677ff;
  font-size: 14px;
  font-weight: 500;
  background-color: #ffffff;
  border: 1px dashed #1677ff;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 8px;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.new-session:hover {
  background-color: #f0f7ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(22, 119, 255, 0.1);
}

/* 右侧聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
  background-color: #ffffff;
  position: relative;
}

.chat-header {
  padding: 20px 28px;
  border-bottom: 1px solid #e8e8e8;
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 16px;
  z-index: 10;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.chat-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333333;
  display: flex;
  align-items: center;
  gap: 10px;
}

.function-tabs {
  width: 100%;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  font-size: 14px;
  color: #333333;
}

.tab-icon {
  font-size: 16px;
}

/* 聊天内容区域 */
.chat-content {
  flex: 1;
  overflow: hidden;
  min-height: 0;
  position: relative;
}

.chat-window {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 24px 48px;
  box-sizing: border-box;
}

/* 聊天输入区域 */
.chat-input {
  flex-shrink: 0;
  border-top: 1px solid #e8e8e8;
  background-color: #ffffff;
  box-shadow: 0 -1px 2px rgba(0, 0, 0, 0.05);
}

.input-box {
  width: 100%;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-container {
    box-shadow: none;
  }
  
  .sidebar {
    display: none;
  }
  
  .chat-header {
    padding: 16px 24px;
    gap: 12px;
  }
  
  .chat-header h2 {
    font-size: 18px;
  }
  
  .chat-window {
    padding: 16px 24px;
  }
}

@media (max-width: 1024px) {
  .sidebar {
    flex: 0 0 30%;
    max-width: 280px;
  }
  
  .chat-window {
    padding: 20px 32px;
  }
}

/* 滚动条样式 */
.session-list::-webkit-scrollbar,
.chat-window::-webkit-scrollbar {
  width: 6px;
}

.session-list::-webkit-scrollbar-track,
.chat-window::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.session-list::-webkit-scrollbar-thumb,
.chat-window::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.session-list::-webkit-scrollbar-thumb:hover,
.chat-window::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>