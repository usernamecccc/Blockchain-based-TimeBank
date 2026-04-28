<template>
  <div class="Box" v-loading="pageLoading" element-loading-text="加载链上数据…">
    <el-alert
      v-if="overview && overview.ready === false"
      :title="'区块链未就绪：' + (overview.reason || '请检查配置与节点')"
      type="warning"
      show-icon
      :closable="false"
      class="chain-alert"
    />
    <el-alert
      v-else-if="overview && overview.ready && overview.eventsError"
      type="warning"
      show-icon
      :closable="false"
      class="chain-alert"
    >
      <template slot="title">
        <span>配置已启用区块链，但<strong>当前无法连上节点</strong>（{{ overview.eventsError }}）。余额与事件将无法加载，数值会为 0。</span>
      </template>
      <div class="alert-detail">
        请在项目 <code>TimeCoinSystem</code> 目录执行 <code>npx hardhat node</code> 保持 <code>127.0.0.1:8545</code> 监听，并确认已部署 TimeCoin 合约、<code>application.properties</code> 中合约地址与私钥一致。
      </div>
    </el-alert>
    <el-alert
      v-else-if="overview && overview.ready && !overview.eventsError"
      title="已成功读取链上数据。链上 userId = 系统用户 ID（字符串）。增发 / 划转需合约 Owner 对应配置的私钥。"
      type="success"
      show-icon
      :closable="false"
      class="chain-alert"
    />

    <div class="toolbar">
      <el-button type="primary" icon="el-icon-refresh" :loading="pageLoading" @click="loadData">
        刷新数据
      </el-button>
    </div>

    <div class="dashboard">
      <h2>链上流通总量（各用户余额之和）: {{ totalCirculatingDisplay }}</h2>
      <div class="summary">
        <p>注册用户数: {{ registeredUsers }}</p>
        <p>持有时间币用户数（余额 &gt; 0）: {{ holdersCount }}</p>
        <p>当前列表 Mint / Transfer 事件数: {{ mintEventCount }} / {{ transferEventCount }}</p>
        <p>{{ recentSummary }}</p>
      </div>
      <div class="charts">
        <div class="pie-chart" ref="pieChart" />
        <div class="bar-chart" ref="barChart" />
      </div>
    </div>

    <el-card class="transaction-history" shadow="hover">
      <div slot="header" class="card-header">
        <span>链上时间币事件（Mint / Transfer）</span>
      </div>
      <el-input v-model="searchQuery" placeholder="搜索类型、金额、双方、Tx" clearable />
      <el-table :data="paginatedTransactions" style="width: 100%; margin-top: 12px">
        <el-table-column prop="time" label="时间" width="170" />
        <el-table-column label="类型" width="120">
          <template slot-scope="scope">
            {{ eventTypeLabel(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="数量" width="120" />
        <el-table-column prop="parties" label="说明" min-width="220" />
        <el-table-column prop="txHash" label="交易哈希" min-width="200">
          <template slot-scope="scope">
            <span class="mono">{{ shortHash(scope.row.txHash) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 12px"
        @size-change="txSizeChange"
        @current-change="txPageChange"
        :current-page="txPage"
        :page-sizes="[5, 10, 20, 50]"
        :page-size="txPageSize"
        layout="total, sizes, prev, pager, next"
        :total="filteredTransactions.length"
      />
    </el-card>

    <el-card class="user-balance-management" shadow="hover">
      <div slot="header" class="card-header">
        <span>用户链上余额</span>
      </div>
      <el-input v-model="userSearchQuery" placeholder="搜索用户名、邮箱、ID" clearable />
      <el-table :data="paginatedUsers" style="width: 100%; margin-top: 12px">
        <el-table-column prop="id" label="用户ID" width="90" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column label="角色" width="100">
          <template slot-scope="scope">
            {{ roleLabel(scope.row.role) }}
          </template>
        </el-table-column>
        <el-table-column label="链上余额" width="120">
          <template slot-scope="scope">
            <span v-if="scope.row.coinBalance != null">{{ scope.row.coinBalance }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" @click="openMintDialog(scope.row)">增发</el-button>
            <el-button type="text" @click="openTransferDialog(scope.row)">作为转出方划转</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 12px"
        @size-change="userSizeChange"
        @current-change="userPageChange"
        :current-page="userPage"
        :page-sizes="[5, 10, 20, 50]"
        :page-size="userPageSize"
        layout="total, sizes, prev, pager, next"
        :total="filteredUsers.length"
      />
    </el-card>

    <el-dialog
      title="链上增发（mint）"
      :visible.sync="mintDialogVisible"
      width="480px"
      append-to-body
    >
      <p v-if="mintTarget" class="dialog-meta">
        用户：{{ mintTarget.username }}（ID {{ mintTarget.id }}，链上 userId = {{ mintTarget.chainUserId }}）
      </p>
      <el-form label-width="100px">
        <el-form-item label="数量">
          <el-input-number v-model="mintAmount" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="mintRemark" type="textarea" :rows="2" placeholder="仅本地说明，不上链" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="mintDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="mintSubmitting" @click="submitMint">确认上链</el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="链上划转（transfer）"
      :visible.sync="transferDialogVisible"
      width="480px"
      append-to-body
    >
      <p v-if="transferFromUser" class="dialog-meta">
        转出方：{{ transferFromUser.username }}（链上 userId = {{ transferFromUser.chainUserId }}）
      </p>
      <el-form label-width="100px">
        <el-form-item label="转入方 userId">
          <el-input v-model="transferToUserId" placeholder="对方用户 ID（字符串）" />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="transferAmount" :min="1" :precision="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="transferDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="transferSubmitting" @click="submitTransfer">确认上链</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import request from '@/utils/request';

export default {
  name: 'CoinView',
  data() {
    return {
      pageLoading: false,
      overview: null,
      users: [],
      transactions: [],
      searchQuery: '',
      userSearchQuery: '',
      userPage: 1,
      userPageSize: 10,
      txPage: 1,
      txPageSize: 10,
      mintDialogVisible: false,
      mintTarget: null,
      mintAmount: 1,
      mintRemark: '',
      mintSubmitting: false,
      transferDialogVisible: false,
      transferFromUser: null,
      transferToUserId: '',
      transferAmount: 1,
      transferSubmitting: false,
      pieChartInstance: null,
      barChartInstance: null,
    };
  },
  computed: {
    registeredUsers() {
      return (this.overview && this.overview.registeredUsers) || 0;
    },
    holdersCount() {
      return (this.overview && this.overview.holdersCount) || 0;
    },
    mintEventCount() {
      return (this.overview && this.overview.mintEventCount) || 0;
    },
    transferEventCount() {
      return (this.overview && this.overview.transferEventCount) || 0;
    },
    totalCirculatingDisplay() {
      if (!this.overview || this.overview.ready === false) return '—';
      return this.overview.totalCirculating != null ? String(this.overview.totalCirculating) : '0';
    },
    recentSummary() {
      const n = this.transactions.length;
      if (!this.overview || this.overview.ready === false) {
        return '链未连接，无法加载事件。dev 配置请将 blockchain.enabled=true 或使用默认 MySQL profile。';
      }
      if (this.overview.eventsError) {
        return '事件加载异常：' + this.overview.eventsError;
      }
      return `当前展示最近 ${n} 条链上记录。`;
    },
    filteredTransactions() {
      const q = (this.searchQuery || '').trim().toLowerCase();
      if (!q) return this.transactions;
      return this.transactions.filter((row) => {
        const blob = [row.type, row.amount, row.parties, row.txHash, row.time]
          .filter(Boolean)
          .join(' ')
          .toLowerCase();
        return blob.includes(q);
      });
    },
    filteredUsers() {
      const q = (this.userSearchQuery || '').trim().toLowerCase();
      if (!q) return this.users;
      return this.users.filter((u) => {
        const blob = [u.id, u.username, u.email, u.chainUserId]
          .filter((x) => x != null && x !== '')
          .join(' ')
          .toLowerCase();
        return blob.includes(q);
      });
    },
    paginatedUsers() {
      const start = (this.userPage - 1) * this.userPageSize;
      return this.filteredUsers.slice(start, start + this.userPageSize);
    },
    paginatedTransactions() {
      const start = (this.txPage - 1) * this.txPageSize;
      return this.filteredTransactions.slice(start, start + this.txPageSize);
    },
  },
  watch: {
    userSearchQuery() {
      this.userPage = 1;
    },
    searchQuery() {
      this.txPage = 1;
    },
  },
  mounted() {
    this.loadData();
  },
  beforeDestroy() {
    if (this.pieChartInstance) this.pieChartInstance.dispose();
    if (this.barChartInstance) this.barChartInstance.dispose();
  },
  methods: {
    roleLabel(role) {
      const r = Number(role);
      const m = { 1: '老人', 2: '志愿者', 3: '管理员' };
      return m[r] || '—';
    },
    eventTypeLabel(t) {
      if (t === 'MINT') return '增发(Mint)';
      if (t === 'TRANSFER') return '划转(Transfer)';
      if (t === 'PARSE_ERROR') return '解析失败';
      return t || '—';
    },
    shortHash(h) {
      if (!h || h.length < 14) return h || '—';
      return `${h.slice(0, 10)}…${h.slice(-6)}`;
    },
    userSizeChange(val) {
      this.userPageSize = val;
      this.userPage = 1;
    },
    userPageChange(val) {
      this.userPage = val;
    },
    txSizeChange(val) {
      this.txPageSize = val;
      this.txPage = 1;
    },
    txPageChange(val) {
      this.txPage = val;
    },
    loadData() {
      this.pageLoading = true;
      request
        .get('/administrator/chain/overview?eventLimit=100')
        .then((res) => {
          if (res.code !== 1 || !res.data) {
            this.$message.error(res.msg || '加载失败');
            return;
          }
          this.overview = res.data;
          this.users = Array.isArray(res.data.users) ? res.data.users : [];
          this.transactions = Array.isArray(res.data.events) ? res.data.events : [];
          this.$nextTick(() => this.updateCharts());
        })
        .catch((e) => {
          console.error(e);
          this.$message.error('请求失败');
        })
        .finally(() => {
          this.pageLoading = false;
        });
    },
    updateCharts() {
      if (!this.$refs.pieChart || !this.$refs.barChart) return;
      const mint = Number(this.mintEventCount) || 0;
      const tr = Number(this.transferEventCount) || 0;
      if (!this.pieChartInstance) this.pieChartInstance = echarts.init(this.$refs.pieChart);
      this.pieChartInstance.setOption({
        title: { text: '链上事件类型', left: 'center' },
        tooltip: { trigger: 'item' },
        series: [
          {
            name: '事件',
            type: 'pie',
            radius: ['40%', '70%'],
            data: [
              { value: mint, name: 'Mint 增发' },
              { value: tr, name: 'Transfer 划转' },
            ],
          },
        ],
      });

      const top = [...this.users]
        .filter((u) => u.coinBalance != null)
        .sort((a, b) => {
          const na = Number(a.coinBalance);
          const nb = Number(b.coinBalance);
          if (Number.isFinite(na) && Number.isFinite(nb)) {
            return nb - na;
          }
          return String(b.coinBalance).localeCompare(String(a.coinBalance), undefined, { numeric: true });
        })
        .slice(0, 5);

      if (!this.barChartInstance) this.barChartInstance = echarts.init(this.$refs.barChart);
      this.barChartInstance.setOption({
        title: { text: '余额 Top5', left: 'center' },
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: top.length ? top.map((u) => u.username || `ID${u.id}`) : ['暂无'],
        },
        yAxis: { type: 'value' },
        series: [
          {
            name: '链上余额',
            type: 'bar',
            data: top.length ? top.map((u) => Number(u.coinBalance)) : [0],
          },
        ],
      });
    },
    openMintDialog(row) {
      if (!this.overview || !this.overview.ready) {
        this.$message.warning('区块链未就绪，无法增发');
        return;
      }
      this.mintTarget = row;
      this.mintAmount = 1;
      this.mintRemark = '';
      this.mintDialogVisible = true;
    },
    submitMint() {
      if (!this.mintTarget) return;
      const amt = Number(this.mintAmount);
      if (!amt || amt < 1) {
        this.$message.warning('请输入有效数量');
        return;
      }
      this.mintSubmitting = true;
      request
        .post('/administrator/chain/mint', {
          userId: String(this.mintTarget.chainUserId || this.mintTarget.id),
          amount: String(Math.floor(amt)),
        })
        .then((res) => {
          if (res.code === 1) {
            const tx = res.data && res.data.txHash ? ` Tx: ${res.data.txHash}` : '';
            this.$message.success('已提交链上交易' + tx);
            this.mintDialogVisible = false;
            this.loadData();
          } else {
            this.$message.error(res.msg || '失败');
          }
        })
        .catch((e) => {
          console.error(e);
          this.$message.error('请求失败');
        })
        .finally(() => {
          this.mintSubmitting = false;
        });
    },
    openTransferDialog(row) {
      if (!this.overview || !this.overview.ready) {
        this.$message.warning('区块链未就绪');
        return;
      }
      this.transferFromUser = row;
      this.transferToUserId = '';
      this.transferAmount = 1;
      this.transferDialogVisible = true;
    },
    submitTransfer() {
      if (!this.transferFromUser) return;
      const to = (this.transferToUserId || '').trim();
      if (!to) {
        this.$message.warning('请填写转入方 userId');
        return;
      }
      const amt = Number(this.transferAmount);
      if (!amt || amt < 1) {
        this.$message.warning('请输入有效数量');
        return;
      }
      this.transferSubmitting = true;
      request
        .post('/administrator/chain/transfer', {
          fromUserId: String(this.transferFromUser.chainUserId || this.transferFromUser.id),
          toUserId: to,
          amount: String(Math.floor(amt)),
        })
        .then((res) => {
          if (res.code === 1) {
            this.$message.success('划转交易已提交');
            this.transferDialogVisible = false;
            this.loadData();
          } else {
            this.$message.error(res.msg || '失败');
          }
        })
        .catch((e) => {
          console.error(e);
          this.$message.error('请求失败');
        })
        .finally(() => {
          this.transferSubmitting = false;
        });
    },
  },
};
</script>

<style scoped>
.Box {
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
}
.chain-alert {
  margin-bottom: 12px;
}
.alert-detail {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
}
.alert-detail code {
  padding: 0 4px;
  background: #f4f4f5;
  border-radius: 3px;
  font-size: 12px;
}
.toolbar {
  margin-bottom: 12px;
}
.dashboard {
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  margin-bottom: 20px;
}
.summary p {
  margin: 6px 0;
  color: #606266;
}
.charts {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;
  flex-wrap: wrap;
}
.pie-chart,
.bar-chart {
  flex: 1;
  min-width: 280px;
  height: 300px;
}
.transaction-history,
.user-balance-management {
  margin-bottom: 20px;
}
.card-header {
  font-weight: 600;
}
.dialog-meta {
  color: #606266;
  font-size: 13px;
  margin: 0 0 12px;
}
.mono {
  font-family: Consolas, monospace;
  font-size: 12px;
}
.text-muted {
  color: #909399;
}
</style>
