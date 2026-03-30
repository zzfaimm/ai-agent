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
  line-height: 1.55;
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