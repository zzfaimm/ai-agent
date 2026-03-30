<template>
  <div class="message-item" :class="{ 'user-message': message.role === 'user', 'ai-message': message.role === 'ai' }">
    <div class="message-content">
      <div class="message-bubble" v-html="renderedContent"></div>
      <div class="message-time">{{ message.timestamp }}</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { marked } from 'marked';

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
});

const renderedContent = computed(() => {
  return marked(props.message.content);
});
</script>

<style scoped>
.message-item {
  margin: 8px 0;
  display: flex;
  max-width: 70%;
  min-width: 30%;
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  margin-left: auto;
  flex-direction: row-reverse;
}

.ai-message {
  margin-right: auto;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.message-bubble {
  padding: 14px 18px;
  border-radius: 20px;
  line-height: 1.6;
  word-wrap: break-word;
  text-align: left;
  max-width: 100%;
  font-size: 14px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.message-bubble:hover {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* Markdown 样式 */
.message-bubble h1,
.message-bubble h2,
.message-bubble h3,
.message-bubble h4,
.message-bubble h5,
.message-bubble h6 {
  margin: 16px 0 8px 0;
  font-weight: 600;
  line-height: 1.4;
}

.message-bubble h1 {
  font-size: 24px;
}

.message-bubble h2 {
  font-size: 20px;
}

.message-bubble h3 {
  font-size: 16px;
}

.message-bubble p {
  margin: 8px 0;
  line-height: 1.6;
}

.message-bubble ul,
.message-bubble ol {
  margin: 8px 0;
  padding-left: 24px;
}

.message-bubble li {
  margin: 4px 0;
  line-height: 1.6;
}

.message-bubble code {
  background-color: #f0f0f0;
  padding: 2px 4px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'Courier New', Courier, monospace;
}

.message-bubble pre {
  background-color: #f0f0f0;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.message-bubble pre code {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
}

.user-message .message-bubble {
  background-color: #1677ff;
  color: white;
  border-bottom-right-radius: 6px;
}

.ai-message .message-bubble {
  background-color: #f5f5f5;
  color: #333;
  border-bottom-left-radius: 6px;
  border: 1px solid #e8e8e8;
}

.message-time {
  font-size: 12px;
  color: #999;
  text-align: right;
  margin-top: 2px;
  font-weight: 400;
}

.user-message .message-time {
  text-align: left;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .message-item {
    max-width: 90%;
  }
  
  .message-bubble {
    padding: 12px 16px;
    font-size: 13px;
  }
}

@media (min-width: 1200px) {
  .message-item {
    max-width: 650px;
  }
}
</style>