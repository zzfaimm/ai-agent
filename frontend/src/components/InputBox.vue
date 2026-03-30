<template>
  <div class="input-box">
    <el-input
      v-model="inputMessage"
      type="textarea"
      :rows="1"
      placeholder="输入你的问题..."
      @keyup.enter.exact="handleSend"
      :disabled="loading"
    />
    <el-button
      type="primary"
      round
      @click="handleSend"
      :loading="loading"
      :disabled="!inputMessage.trim() || loading"
    >
      发送
    </el-button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['send']);

const inputMessage = ref('');

const handleSend = () => {
  const message = inputMessage.value.trim();
  if (message && !props.loading) {
    emit('send', message);
    inputMessage.value = '';
  }
};

// 自动调整输入框高度
watch(inputMessage, () => {
  adjustTextareaHeight();
});

const adjustTextareaHeight = () => {
  const textarea = document.querySelector('.el-textarea__inner');
  if (textarea) {
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px';
  }
};
</script>

<style scoped>
.input-box {
  display: flex;
  gap: 10px;
  padding: 16px;
  background-color: #fff;
  border-top: 1px solid #e8e8e8;
  border-radius: 0 0 8px 8px;
  align-items: flex-end;
}

.el-input {
  flex: 1;
  min-height: 40px;
}

.el-textarea__inner {
  border-radius: 20px;
  resize: none;
  transition: height 0.2s ease;
}

.el-button {
  flex-shrink: 0;
  height: 40px;
  padding: 0 20px;
  border-radius: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .input-box {
    padding: 12px;
  }
}
</style>