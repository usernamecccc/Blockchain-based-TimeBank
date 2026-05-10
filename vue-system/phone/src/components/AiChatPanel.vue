<template>
  <div class="ai-chat-panel">
    <div class="chat-header">
      <div class="header-title">{{ title }}</div>
      <div class="header-subtitle">有问题可以直接问我</div>
    </div>

    <div ref="messageList" class="message-list">
      <div
        v-for="(item, index) in messages"
        :key="index"
        class="message-row"
        :class="[item.role, { pending: item.pending }]"
      >
        <div class="message-bubble">
          <span v-html="formatMessage(item.content)"></span>
          <span v-if="item.pending" class="typing-dots">
            <span></span><span></span><span></span>
          </span>
        </div>
      </div>
    </div>

    <div class="quick-actions">
      <el-button
        v-for="item in quickQuestions"
        :key="item"
        size="mini"
        round
        plain
        @click="askQuickQuestion(item)"
      >
        {{ item }}
      </el-button>
    </div>

    <div class="input-bar">
      <el-input
        v-model="input"
        type="textarea"
        :autosize="{ minRows: 1, maxRows: 3 }"
        placeholder="请输入你的问题"
        :disabled="loading"
        @keyup.enter.native="sendMessage"
      />
      <el-button type="primary" round :loading="loading" @click="sendMessage">发送</el-button>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request';

export default {
  name: 'AiChatPanel',
  props: {
    userType: {
      type: String,
      required: true,
    },
    title: {
      type: String,
      default: 'AI助手',
    },
  },
  data() {
    return {
      input: '',
      loading: false,
      messages: [
        {
          role: 'assistant',
          content: '你好，我是时间银行AI助手。你可以咨询活动、报名、签到、时间币或平台使用问题。',
        },
      ],
      quickQuestions: ['怎么报名活动', '如何签到', '时间币怎么查看'],
    };
  },
  methods: {
    endpoint() {
      return this.userType === 'volunteer' ? '/users/vol/ai/chat' : '/users/old/ai/chat';
    },
    askQuickQuestion(question) {
      this.input = question;
      this.sendMessage();
    },
    escapeHtml(text) {
      return String(text || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    },
    formatMessage(content) {
      let html = this.escapeHtml(content);
      html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
      html = html.replace(/\n/g, '<br>');
      return html;
    },
    sendMessage() {
      const text = (this.input || '').trim();
      if (!text || this.loading) return;

      this.messages.push({ role: 'user', content: text });
      this.input = '';
      this.loading = true;
      const pendingIndex = this.messages.push({
        role: 'assistant',
        content: '正在思考，请稍候',
        pending: true,
      }) - 1;
      this.scrollToBottom();

      request.post(this.endpoint(), { message: text }, { timeout: 260000 })
        .then(response => {
          if (response.code === 1 && response.data && response.data.reply) {
            this.$set(this.messages, pendingIndex, { role: 'assistant', content: response.data.reply });
          } else {
            this.$set(this.messages, pendingIndex, { role: 'assistant', content: response.msg || 'AI回复失败，请稍后重试。' });
          }
        })
        .catch(() => {
          this.$set(this.messages, pendingIndex, { role: 'assistant', content: 'AI服务暂时不可用，请稍后重试。' });
        })
        .finally(() => {
          this.loading = false;
          this.scrollToBottom();
        });
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.messageList;
        if (el) {
          el.scrollTop = el.scrollHeight;
        }
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.ai-chat-panel {
  min-height: calc(100vh - 82px);
  box-sizing: border-box;
  padding: 14px 14px 86px;
  background: #f5f7fb;

  .chat-header {
    padding: 16px;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 2px 10px rgba(31, 45, 61, 0.08);

    .header-title {
      font-size: 20px;
      font-weight: 700;
      color: #303133;
      line-height: 1.3;
    }

    .header-subtitle {
      margin-top: 4px;
      font-size: 13px;
      color: #909399;
    }
  }

  .message-list {
    height: calc(100vh - 282px);
    min-height: 280px;
    margin-top: 12px;
    padding: 12px;
    overflow-y: auto;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 2px 10px rgba(31, 45, 61, 0.08);
  }

  .message-row {
    display: flex;
    margin-bottom: 10px;

    .message-bubble {
      max-width: 78%;
      padding: 10px 12px;
      border-radius: 8px;
      font-size: 15px;
      line-height: 1.55;
      word-break: break-word;
      white-space: pre-wrap;
    }

    &.assistant {
      justify-content: flex-start;

      .message-bubble {
        color: #303133;
        background: #eef3ff;
      }

      &.pending .message-bubble {
        color: #606266;
      }
    }

    &.user {
      justify-content: flex-end;

      .message-bubble {
        color: #ffffff;
        background: #409eff;
      }
    }
  }

  .typing-dots {
    display: inline-flex;
    gap: 3px;
    margin-left: 6px;
    vertical-align: middle;

    span {
      width: 4px;
      height: 4px;
      border-radius: 50%;
      background: #909399;
      animation: typing-bounce 1.2s infinite ease-in-out;
    }

    span:nth-child(2) {
      animation-delay: 0.16s;
    }

    span:nth-child(3) {
      animation-delay: 0.32s;
    }
  }

  @keyframes typing-bounce {
    0%, 80%, 100% {
      transform: translateY(0);
      opacity: 0.45;
    }
    40% {
      transform: translateY(-3px);
      opacity: 1;
    }
  }

  .quick-actions {
    display: flex;
    gap: 8px;
    padding: 10px 0;
    overflow-x: auto;

    .el-button {
      flex-shrink: 0;
    }
  }

  .input-bar {
    display: flex;
    gap: 8px;
    align-items: flex-end;
    padding: 10px;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: 0 2px 10px rgba(31, 45, 61, 0.08);

    .el-textarea {
      flex: 1;
    }

    .el-button {
      flex-shrink: 0;
    }
  }
}
</style>
