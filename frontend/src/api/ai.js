import axios from 'axios';

// 创建 axios 实例
const api = axios.create({
  baseURL: '/api',
  timeout: 300000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 多Agent生成接口（普通请求）
export const generateAgent = async (message) => {
  try {
    console.log('调用多Agent接口:', message);
    const response = await api.post('/ai/agent/generate', {
      input: message
    });
    console.log('多Agent接口响应:', response.data);
    return response.data;
  } catch (error) {
    console.error('生成失败:', error);
    throw error;
  }
};

// LoveApp 流式聊天接口（SSE）
export const loveAppChat = (message, chatId, onMessage, onError, onComplete) => {
  const url = `/api/ai/love_app/chat/sse?message=${encodeURIComponent(message)}&chatId=${chatId}`;
  console.log('调用恋爱机器人接口:', url);
  const eventSource = new EventSource(url);

  eventSource.onmessage = (event) => {
    console.log('收到恋爱机器人消息:', event.data);
    onMessage(event.data);
  };

  eventSource.onerror = (error) => {
    // 检查是否是正常关闭
    if (eventSource.readyState === EventSource.CLOSED) {
      console.log('SSE 连接正常关闭');
      onComplete();
    } else {
      console.error('SSE 错误:', error);
      onError(error);
    }
    eventSource.close();
  };

  eventSource.onclose = () => {
    console.log('SSE 连接关闭');
    // 这里不再调用 onComplete，因为 onerror 中已经处理了
  };

  return eventSource;
};

// Manus 超级Agent（SSE）
export const manusChat = (message, chatId, onMessage, onError, onComplete) => {
  const url = `/api/ai/manus/chat?message=${encodeURIComponent(message)}&chatId=${chatId}`;
  console.log('调用超级智能体接口:', url);
  const eventSource = new EventSource(url);

  eventSource.onmessage = (event) => {
    console.log('收到超级智能体消息:', event.data);
    onMessage(event.data);
  };

  eventSource.onerror = (error) => {
    // 检查是否是正常关闭
    if (eventSource.readyState === EventSource.CLOSED) {
      console.log('SSE 连接正常关闭');
      onComplete();
    } else {
      console.error('SSE 错误:', error);
      onError(error);
    }
    eventSource.close();
  };

  eventSource.onclose = () => {
    console.log('SSE 连接关闭');
    // 这里不再调用 onComplete，因为 onerror 中已经处理了
  };

  return eventSource;
};

export default {
  generateAgent,
  loveAppChat,
  manusChat
};