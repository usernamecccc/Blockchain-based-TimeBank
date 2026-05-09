<template>
    <div class="mobile-app">
      <div class="user-info" v-loading="loading">
        <el-avatar class="avatar" size="large" :src="avatarSrc">用户</el-avatar>
        <div class="details">
          <p>用户名：{{ userInfo.username || '—' }}</p>
          <p>时间币余额（链上）：{{ coinBalanceDisplay }}</p>
          <p v-if="!coinChainReady && chainReason" class="hint">{{ chainReason }}</p>
          <p>信誉分数：暂无记录</p>
        </div>
      </div>

      <div class="transaction-history">
        <h2>说明</h2>
        <el-card class="transaction-card">
          <p class="muted">链上 Mint / Transfer 流水请在 PC 管理端「时间币管理」查看；此处展示当前账号在合约中的余额。</p>
        </el-card>
      </div>
    </div>
  </template>

  <script>
  import request from '@/utils/request';

  export default {
    data() {
      return {
        loading: true,
        userInfo: {},
        coinBalance: null,
        coinChainReady: true,
        chainReason: '',
      };
    },
    computed: {
      avatarSrc() {
        const img = this.userInfo.image;
        if (!img) return '';
        if (img.startsWith('http')) return img;
        return `http://localhost:8080/image/${img}`;
      },
      coinBalanceDisplay() {
        if (this.loading) return '…';
        if (this.coinBalance === null || this.coinBalance === undefined) return '—';
        return String(this.coinBalance);
      },
    },
    created() {
      this.load();
    },
    methods: {
      load() {
        this.loading = true;
        Promise.all([
          request.get('/info'),
          request.get('/info/coinBalance'),
        ])
          .then(([infoRes, coinRes]) => {
            if (infoRes.code === 1 && infoRes.data) {
              this.userInfo = infoRes.data;
            }
            if (coinRes.code === 1 && coinRes.data) {
              this.coinBalance = coinRes.data.balance != null ? String(coinRes.data.balance) : '0';
              this.coinChainReady = coinRes.data.chainReady !== false;
              this.chainReason = coinRes.data.reason || '';
            } else {
              this.coinBalance = '—';
            }
          })
          .catch(() => {
            this.$message.error('加载失败');
            this.coinBalance = '—';
          })
          .finally(() => {
            this.loading = false;
          });
      },
    },
  };
  </script>

  <style scoped>
  .mobile-app {
    padding: 10px;
  }

  .user-info {
    display: flex;
    align-items: flex-start;
    padding: 20px;
    background-color: #f5f5f5;
  }

  .avatar {
    margin-right: 20px;
    flex-shrink: 0;
  }

  .details p {
    margin: 6px 0;
  }

  .hint {
    font-size: 12px;
    color: #e6a23c;
  }

  .transaction-history {
    margin-top: 20px;
  }

  .transaction-card {
    margin-bottom: 10px;
  }

  .muted {
    color: #666;
    line-height: 1.5;
    margin: 0;
  }
  </style>
