<template>
    <div class="mobile-app">
      <div class="header" v-loading="loading">
        <h2>转账历史</h2>
        <el-button type="text" icon="el-icon-refresh" @click="loadHistory(1)" :disabled="loading">刷新</el-button>
      </div>

      <el-alert
        v-if="!coinChainReady && chainReason"
        :title="'区块链未就绪：' + chainReason"
        type="warning"
        show-icon
        :closable="false"
        class="chain-alert"
      />

      <div class="history-list" v-loading="loading">
        <el-empty v-if="!loading && total === 0" description="暂无转账记录" />
        
        <div v-else>
          <div class="history-item" v-for="(item, index) in history" :key="index">
            <div class="item-left">
              <div class="item-icon" :class="item.direction === 'IN' ? 'income' : 'outcome'">
                <i :class="item.direction === 'IN' ? 'el-icon-top' : 'el-icon-bottom'"></i>
              </div>
              <div class="item-info">
                <div class="item-title">
                  <span v-if="item.type === 'MINT'">平台发放</span>
                  <span v-else>
                    <span v-if="item.direction === 'IN'">
                      <span v-if="item.counterparty === '平台'">来自 {{ item.counterparty }}</span>
                      <span v-else>
                        来自用户 {{ item.counterpartyName || item.counterparty }}
                        <span>(ID：{{ item.counterparty }})</span>
                      </span>
                    </span>
                    <span v-else>
                      <span v-if="item.counterparty === '平台'">转给 {{ item.counterparty }}</span>
                      <span v-else>
                        转给用户 {{ item.counterpartyName || item.counterparty }}
                        <span>(ID：{{ item.counterparty }})</span>
                      </span>
                    </span>
                  </span>
                </div>
                <div class="item-time">{{ item.time }}</div>
              </div>
            </div>
            <div class="item-right" :class="item.direction === 'IN' ? 'income' : 'outcome'">
              <span v-if="item.direction === 'IN'">+</span>
              <span v-else>-</span>
              {{ item.amount }}
            </div>
          </div>

          <div class="pagination">
            <div class="pagination-info">
              共 {{ total }} 条，第 {{ page }}/{{ totalPages }} 页
            </div>
            <div class="pagination-buttons">
              <el-button 
                size="mini" 
                :disabled="page <= 1 || loading"
                @click="loadHistory(page - 1)">
                <i class="el-icon-arrow-left"></i> 上一页
              </el-button>
              <el-button 
                size="mini" 
                type="primary"
                :disabled="page >= totalPages || loading"
                @click="loadHistory(page + 1)">
                下一页 <i class="el-icon-arrow-right"></i>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </template>

  <script>
  import request from '@/utils/request';

  export default {
    data() {
      return {
        loading: false,
        history: [],
        total: 0,
        page: 1,
        pageSize: 10,
        totalPages: 1,
        coinChainReady: true,
        chainReason: ''
      };
    },
    created() {
      this.loadHistory(1);
    },
    methods: {
      loadHistory(targetPage) {
        this.loading = true;
        Promise.all([
          request.get(`/info/coin/history/paged?page=${targetPage}&pageSize=${this.pageSize}`),
          request.get('/info/coinBalance')
        ])
          .then(([historyRes, coinRes]) => {
            if (historyRes.code === 1 && historyRes.data) {
              this.history = historyRes.data.rows || [];
              this.total = historyRes.data.total || 0;
              this.page = historyRes.data.page || 1;
              this.pageSize = historyRes.data.pageSize || 10;
              this.totalPages = historyRes.data.totalPages || 1;
            }
            if (coinRes.code === 1 && coinRes.data) {
              this.coinChainReady = coinRes.data.chainReady !== false;
              this.chainReason = coinRes.data.reason || '';
            }
          })
          .catch(() => {
            this.$message.error('加载失败');
            this.history = [];
            this.total = 0;
          })
          .finally(() => {
            this.loading = false;
          });
      }
    }
  };
  </script>

  <style scoped>
  .mobile-app {
    padding: 10px;
  }

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .header h2 {
    margin: 0;
    font-size: 20px;
  }

  .chain-alert {
    margin-bottom: 16px;
  }

  .history-list {
    background: #fff;
    border-radius: 8px;
    padding: 10px 0;
  }

  .history-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;
  }

  .history-item:last-child {
    border-bottom: none;
  }

  .item-left {
    display: flex;
    align-items: center;
    flex: 1;
  }

  .item-icon {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
    font-size: 18px;
  }

  .item-icon.income {
    background: #e1f3d8;
    color: #67c23a;
  }

  .item-icon.outcome {
    background: #fef0f0;
    color: #f56c6c;
  }

  .item-info {
    flex: 1;
  }

  .item-title {
    font-size: 15px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 4px;
  }

  .item-time {
    font-size: 13px;
    color: #909399;
  }

  .item-right {
    font-size: 18px;
    font-weight: 600;
  }

  .item-right.income {
    color: #67c23a;
  }

  .item-right.outcome {
    color: #f56c6c;
  }

  .pagination {
    padding: 16px;
    border-top: 1px solid #f0f0f0;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .pagination-info {
    text-align: center;
    font-size: 13px;
    color: #909399;
  }

  .pagination-buttons {
    display: flex;
    justify-content: center;
    gap: 12px;
  }
  </style>
