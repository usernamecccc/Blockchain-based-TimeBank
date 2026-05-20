<template>
  <div class="mobile-app">
    <div class="user-info" v-loading="loading">
      <el-avatar class="avatar" size="large" :src="avatarSrc">用户</el-avatar>
      <div class="details">
        <p>用户名：{{ userInfo.username || '—' }}</p>
        <p class="user-id-line">用户 ID：{{ userInfo.id != null && userInfo.id !== '' ? userInfo.id : '—' }}</p>
        <p>时间币余额（链上）：<strong class="coin-balance">{{ coinBalanceDisplay }}</strong></p>
        <p v-if="!coinChainReady && chainReason" class="hint">{{ chainReason }}</p>
      </div>
    </div>

    <div class="action-buttons">
      <el-button type="primary" size="large" @click="openTransferDialog" :disabled="!coinChainReady">
        <i class="el-icon-s-finance" style="margin-right: 8px;"></i>
        转账时间币
      </el-button>
      <el-button size="large" @click="load">
        <i class="el-icon-refresh" style="margin-right: 8px;"></i>
        刷新余额
      </el-button>
    </div>

    <div class="action-buttons">
      <el-button type="success" size="large" @click="goToHistory" :disabled="!coinChainReady">
        <i class="el-icon-document" style="margin-right: 8px;"></i>
        查看转账历史
      </el-button>
    </div>

    <div class="transaction-history">
      <h2>说明</h2>
      <el-card class="transaction-card">
        <p class="muted">链上 Mint / Transfer 流水请在 PC 管理端「时间币管理」查看；此处展示当前账号在合约中的余额。</p>
      </el-card>
    </div>

    <el-dialog title="转账时间币" :visible.sync="transferDialogVisible" width="90%" :before-close="cancelTransfer">
      <el-form :model="transferForm" label-width="80px">
        <el-form-item label="当前余额">
          <span class="current-balance">{{ coinBalanceDisplay }} 时间币</span>
        </el-form-item>
        <el-form-item label="收款用户">
          <el-input v-model="transferForm.toUserId" placeholder="请输入对方用户 ID" type="number"
            :disabled="transferSubmitting"></el-input>
        </el-form-item>
        <el-form-item label="转账数量">
          <el-input-number v-model="transferForm.amount" :min="1" :max="Number(coinBalance || 0)" :precision="0"
            :step="1" style="width: 100%" :disabled="transferSubmitting"></el-input-number>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="transferForm.password" type="password" placeholder="请输入登录密码进行验证" show-password
            :disabled="transferSubmitting"></el-input>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="transferForm.remark" type="textarea" :rows="2" placeholder="可选，仅本地说明"
            :disabled="transferSubmitting"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="cancelTransfer" :disabled="transferSubmitting">取消</el-button>
        <el-button type="primary" :loading="transferSubmitting" @click="submitTransfer">确认转账</el-button>
      </span>
    </el-dialog>
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
      transferDialogVisible: false,
      transferSubmitting: false,
      transferForm: {
        toUserId: '',
        amount: 1,
        password: '',
        remark: ''
      }
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
    }
  },
  created() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      Promise.all([
        request.get('/info'),
        request.get('/info/coinBalance')
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
    goToHistory() {
      this.$router.push('/coinHistory');
    },
    openTransferDialog() {
      if (!this.coinChainReady) {
        this.$message.warning('区块链未就绪，无法转账');
        return;
      }
      if (!this.coinBalance || Number(this.coinBalance) <= 0) {
        this.$message.warning('余额不足，无法转账');
        return;
      }
      this.transferForm = {
        toUserId: '',
        amount: 1,
        password: '',
        remark: ''
      };
      this.transferDialogVisible = true;
    },
    cancelTransfer() {
      this.transferDialogVisible = false;
      this.transferForm = {
        toUserId: '',
        amount: 1,
        password: '',
        remark: ''
      };
    },
    submitTransfer() {
      if (!this.transferForm.toUserId || !this.transferForm.toUserId.trim()) {
        this.$message.warning('请输入收款用户 ID');
        return;
      }
      if (!this.transferForm.amount || this.transferForm.amount <= 0) {
        this.$message.warning('请输入有效转账数量');
        return;
      }
      if (Number(this.transferForm.amount) > Number(this.coinBalance || 0)) {
        this.$message.warning('余额不足');
        return;
      }
      if (!this.transferForm.password || !this.transferForm.password.trim()) {
        this.$message.warning('请输入登录密码进行验证');
        return;
      }
      this.transferSubmitting = true;
      request
        .post('/info/coin/transfer', {
          toUserId: this.transferForm.toUserId.trim(),
          amount: String(this.transferForm.amount),
          password: this.transferForm.password.trim()
        })
        .then((res) => {
          if (res.code === 1) {
            this.$message.success('转账成功！');
            this.transferDialogVisible = false;
            this.load();
          } else {
            this.$message.error(res.msg || '转账失败');
          }
        })
        .catch((e) => {
          console.error(e);
          this.$message.error('请求失败');
        })
        .finally(() => {
          this.transferSubmitting = false;
        });
    }
  }
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
  border-radius: 8px;
  margin-bottom: 16px;
}

.avatar {
  margin-right: 20px;
  flex-shrink: 0;
}

.details p {
  margin: 6px 0;
}

.user-id-line {
  font-size: 13px;
  color: #909399;
}

.coin-balance {
  font-size: 18px;
  color: #409EFF;
}

.hint {
  font-size: 12px;
  color: #e6a23c;
}

.action-buttons {
  margin-bottom: 16px;
  display: flex;
  gap: 10px;
}

.action-buttons .el-button {
  flex: 1;
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

.current-balance {
  font-size: 16px;
  font-weight: 600;
  color: #409EFF;
}
</style>
