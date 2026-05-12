<template>
  <div class="Box" v-loading="pageLoading">
    <el-alert
      title="平台追讨记录：老人撤回时，平台已垫付退还老人时间币。管理员可在此管理追讨状态。"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <div class="toolbar">
      <el-select
        v-model="filterStatus"
        placeholder="选择状态筛选"
        clearable
        style="width: 200px; margin-right: 12px"
        @change="loadData"
      >
        <el-option label="全部" :value="null"></el-option>
        <el-option label="待追讨" :value="0"></el-option>
        <el-option label="已追讨" :value="1"></el-option>
        <el-option label="平台核销" :value="2"></el-option>
      </el-select>
      <el-button type="primary" icon="el-icon-refresh" :loading="pageLoading" @click="loadData">
        刷新
      </el-button>
    </div>

    <el-card shadow="hover">
      <div slot="header" class="card-header">
        <span>追讨记录（{{ recordList.length }} 条）</span>
      </div>

      <el-table :data="recordList" style="width: 100%">
        <el-table-column prop="id" label="记录ID" width="80" />
        <el-table-column prop="activityId" label="活动ID" width="90" />
        <el-table-column prop="volunteerTableId" label="志愿者表ID" width="110" />
        <el-table-column prop="elderUserId" label="老人用户ID" width="110" />
        <el-table-column prop="amount" label="垫付金额" width="100">
          <template slot-scope="scope">
            <span style="color: #f56c6c; font-weight: 600">
              {{ scope.row.amount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template slot-scope="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">
              {{ statusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-select
              v-if="scope.row.status === 0"
              v-model="scope.row._tempStatus"
              placeholder="选择操作"
              size="small"
              style="width: 140px"
              @change="handleStatusChange(scope.row)"
            >
              <el-option label="已追讨" :value="1"></el-option>
              <el-option label="平台核销" :value="2"></el-option>
            </el-select>
            <span v-else style="color: #909399; font-size: 12px">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!recordList.length" class="empty-tip">
        暂无追讨记录
      </div>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request';

export default {
  name: 'CompensationView',
  data() {
    return {
      pageLoading: false,
      recordList: [],
      filterStatus: null,
    };
  },
  mounted() {
    this.loadData();
  },
  methods: {
    statusLabel(s) {
      const m = { 0: '待追讨', 1: '已追讨', 2: '平台核销' };
      return m[Number(s)] || '—';
    },
    statusType(s) {
      const n = Number(s);
      if (n === 0) return 'warning';
      if (n === 1) return 'success';
      if (n === 2) return 'info';
      return '';
    },
    loadData() {
      this.pageLoading = true;
      const params = this.filterStatus != null ? { status: this.filterStatus } : {};
      request
        .get('/administrator/compensation/list', { params })
        .then((res) => {
          if (res.code !== 1 || !res.data) {
            this.$message.error(res.msg || '加载失败');
            this.recordList = [];
            return;
          }
          this.recordList = Array.isArray(res.data.list) ? res.data.list : [];
        })
        .catch((e) => {
          console.error(e);
          this.$message.error('请求失败');
        })
        .finally(() => {
          this.pageLoading = false;
        });
    },
    handleStatusChange(row) {
      if (!row._tempStatus) return;
      const statusText = row._tempStatus === 1 ? '已追讨' : '平台核销';
      this.$confirm(`确定要将该记录标记为「${statusText}」吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          request
            .post(`/administrator/compensation/process/${row.id}`, { status: row._tempStatus })
            .then((res) => {
              if (res.code === 1) {
                this.$message.success('操作成功');
                row.status = row._tempStatus;
                row._tempStatus = undefined;
              } else {
                this.$message.error(res.msg || '操作失败');
                row._tempStatus = undefined;
              }
            })
            .catch((e) => {
              console.error(e);
              this.$message.error('请求失败');
              row._tempStatus = undefined;
            });
        })
        .catch(() => {
          row._tempStatus = undefined;
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
.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
.card-header {
  font-weight: 600;
}
.empty-tip {
  padding: 40px 0;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>
