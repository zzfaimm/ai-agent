import { defineStore } from 'pinia';

// 生成唯一会话ID
const generateSessionId = (agentType) => {
  return `${agentType}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
};

// 从本地存储加载数据
const loadFromStorage = () => {
  try {
    const data = localStorage.getItem('ai-chat-sessions');
    return data ? JSON.parse(data) : {
      activeAgent: 'loveApp',
      sessions: {
        loveApp: [],
        manus: [],
        agent: []
      },
      messages: {
        loveApp: {},
        manus: {},
        agent: {}
      }
    };
  } catch (error) {
    console.error('Failed to load data from storage:', error);
    return {
      activeAgent: 'loveApp',
      sessions: {
        loveApp: [],
        manus: [],
        agent: []
      },
      messages: {
        loveApp: {},
        manus: {},
        agent: {}
      }
    };
  }
};

// 保存数据到本地存储
const saveToStorage = (state) => {
  try {
    localStorage.setItem('ai-chat-sessions', JSON.stringify(state));
  } catch (error) {
    console.error('Failed to save data to storage:', error);
  }
};

export const useChatStore = defineStore('chat', {
  state: () => loadFromStorage(),
  
  actions: {
    // 切换智能体
    setActiveAgent(agentType) {
      this.activeAgent = agentType;
      saveToStorage(this.$state);
    },
    
    // 创建新会话
    createSession(agentType) {
      const sessionId = generateSessionId(agentType);
      const newSession = {
        id: sessionId,
        title: '新会话',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      
      this.sessions[agentType].unshift(newSession);
      this.messages[agentType][sessionId] = [];
      saveToStorage(this.$state);
      
      return sessionId;
    },
    
    // 更新会话标题
    updateSessionTitle(agentType, sessionId, title) {
      const sessionIndex = this.sessions[agentType].findIndex(s => s.id === sessionId);
      if (sessionIndex !== -1) {
        this.sessions[agentType][sessionIndex].title = title;
        this.sessions[agentType][sessionIndex].updatedAt = new Date().toISOString();
        saveToStorage(this.$state);
      }
    },
    
    // 添加消息
    addMessage(agentType, sessionId, message) {
      if (!this.messages[agentType][sessionId]) {
        this.messages[agentType][sessionId] = [];
      }
      
      this.messages[agentType][sessionId].push({
        ...message,
        timestamp: new Date().toISOString()
      });
      
      // 更新会话时间
      const sessionIndex = this.sessions[agentType].findIndex(s => s.id === sessionId);
      if (sessionIndex !== -1) {
        this.sessions[agentType][sessionIndex].updatedAt = new Date().toISOString();
      }
      
      saveToStorage(this.$state);
    },
    
    // 更新AI消息内容（用于流式输出）
    updateAiMessage(agentType, sessionId, messageId, content) {
      const messages = this.messages[agentType][sessionId];
      if (messages) {
        const messageIndex = messages.findIndex(m => m.id === messageId);
        if (messageIndex !== -1) {
          messages[messageIndex].content = content;
          saveToStorage(this.$state);
        }
      }
    },
    
    // 获取会话消息
    getSessionMessages(agentType, sessionId) {
      return this.messages[agentType][sessionId] || [];
    },
    
    // 获取当前智能体的会话列表
    getCurrentSessions() {
      return this.sessions[this.activeAgent] || [];
    },
    
    // 清除所有数据
    clearAllData() {
      this.$state = {
        activeAgent: 'loveApp',
        sessions: {
          loveApp: [],
          manus: [],
          agent: []
        },
        messages: {
          loveApp: {},
          manus: {},
          agent: {}
        }
      };
      saveToStorage(this.$state);
    }
  }
});