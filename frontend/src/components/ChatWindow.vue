<template>
  <div class="chat-window" ref="chatWindowRef">
    <div class="message-list">
      <MessageItem
        v-for="message in messageList"
        :key="message.id"
        :message="message"
      />
      <div v-if="loading" class="loading">
        <el-icon class="is-loading"><svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24"><path fill="currentColor" d="M12 3a9 9 0 1 0 9 9a9 9 0 0 0-9-9m0 16a7 7 0 1 1 7-7a7 7 0 0 1-7 7m1-13a1 1 0 0 1 1 1v4a1 1 0 1 1-2 0V7a1 1 0 0 1 1-1m0 10a1 1 0 1 1 0-2a1 1 0 0 1 0 2Z"/></svg></el-icon>
        <span>AI 正在思考...</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue';
import MessageItem from './MessageItem.vue';

const props = defineProps({
  messageList: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const chatWindowRef = ref(null);

// 自动滚动到底部
const scrollToBottom = async () => {
  await nextTick();
  if (chatWindowRef.value) {
    chatWindowRef.value.scrollTop = chatWindowRef.value.scrollHeight;
  }
};

// 监听消息列表变化，自动滚动
watch(
  () => props.messageList.length,
  () => {
    scrollToBottom();
  }
);

// 监听加载状态变化，自动滚动
watch(
  () => props.loading,
  () => {
    scrollToBottom();
  }
);

// 初始滚动到底部
scrollToBottom();
</script>

<style scoped>
.chat-window {
  flex: 1;
  overflow-y: auto;
  padding: 24px 48px;
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  position: relative;
}

/* 添加渐变背景效果 */
.chat-window::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 120px;
  background: linear-gradient(to bottom, rgba(245, 245, 245, 0.8), rgba(245, 245, 245, 0));
  pointer-events: none;
  z-index: 1;
}

.message-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 24px;
  box-sizing: border-box;
  position: relative;
  z-index: 2;
}

.loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  margin: 10px 0;
  font-size: 14px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0.6;
  }
}

/* 滚动条样式 */
.chat-window::-webkit-scrollbar {
  width: 6px;
}

.chat-window::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.chat-window::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.chat-window::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-window {
    padding: 12px;
  }
}
</style>