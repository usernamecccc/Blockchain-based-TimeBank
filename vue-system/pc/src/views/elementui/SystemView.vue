<template>
  <div class="system-info" v-loading="loading" element-loading-text="正在采集本机指标…">
    <div class="toolbar">
      <el-button type="primary" icon="el-icon-refresh" :loading="loading" @click="loadMetrics">
        刷新
      </el-button>
      <span v-if="collectedAt" class="meta">采集时间：{{ collectedAt }}</span>
    </div>

    <el-alert
      v-if="errorMsg"
      :title="errorMsg"
      type="error"
      show-icon
      :closable="false"
      class="mb"
    />
    <el-alert
      v-else
      title="数据来自运行后端的这台机器（Java 进程所在主机），非浏览器所在电脑。"
      type="info"
      show-icon
      :closable="false"
      class="mb"
    />

    <div class="cards-row">
      <el-card class="box-card">
        <div slot="header" class="clearfix">
          <span>CPU</span>
        </div>
        <el-table :data="cpuRows" style="width: 100%">
          <el-table-column prop="name" label="属性" />
          <el-table-column prop="value" label="值" />
        </el-table>
      </el-card>

      <el-card class="box-card">
        <div slot="header" class="clearfix">
          <span>内存</span>
        </div>
        <el-table :data="memoryRows" style="width: 100%">
          <el-table-column prop="name" label="属性" />
          <el-table-column prop="value" label="值" />
        </el-table>
      </el-card>
    </div>

    <el-card class="box-card wide">
      <div slot="header" class="clearfix">
        <span>服务器 / 操作系统</span>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="主机名">{{ server.hostname || '—' }}</el-descriptions-item>
        <el-descriptions-item label="本机 IPv4（非回环）">
          {{ serverIpsDisplay }}
        </el-descriptions-item>
        <el-descriptions-item label="系统">{{ server.osName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="内核/版本">{{ server.osVersion || '—' }}</el-descriptions-item>
        <el-descriptions-item label="架构">{{ server.arch || '—' }}</el-descriptions-item>
        <el-descriptions-item label="运行时长">{{ uptimeText }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="box-card wide">
      <div slot="header" class="clearfix">
        <span>JVM 内存（当前进程）</span>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="Java 版本">{{ jvm.javaVersion || '—' }}</el-descriptions-item>
        <el-descriptions-item label="最大堆 -Xmx 上限">{{ formatBytes(jvm.maxMemoryBytes) }}</el-descriptions-item>
        <el-descriptions-item label="已申请堆">{{ formatBytes(jvm.totalMemoryBytes) }}</el-descriptions-item>
        <el-descriptions-item label="堆空闲">{{ formatBytes(jvm.freeMemoryBytes) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="box-card wide">
      <div slot="header" class="clearfix">
        <span>磁盘分区</span>
      </div>
      <el-table :data="disks" style="width: 100%">
        <el-table-column prop="mount" label="挂载点" min-width="100" />
        <el-table-column prop="label" label="描述" min-width="140" show-overflow-tooltip />
        <el-table-column prop="fsType" label="文件系统" width="100" />
        <el-table-column label="总容量" min-width="100">
          <template slot-scope="scope">{{ formatBytes(scope.row.totalBytes) }}</template>
        </el-table-column>
        <el-table-column label="可用" min-width="100">
          <template slot-scope="scope">{{ formatBytes(scope.row.usableBytes) }}</template>
        </el-table-column>
        <el-table-column label="已用" min-width="100">
          <template slot-scope="scope">{{ formatBytes(scope.row.usedBytes) }}</template>
        </el-table-column>
        <el-table-column prop="usedPercent" label="已用%" width="90" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request';

export default {
  name: 'SystemView',
  data() {
    return {
      loading: false,
      errorMsg: '',
      collectedAt: '',
      server: {},
      cpu: {},
      memory: {},
      jvm: {},
      disks: [],
    };
  },
  computed: {
    serverIpsDisplay() {
      const ips = this.server.primaryIpv4s;
      if (Array.isArray(ips) && ips.length) return ips.join('，');
      return '—';
    },
    uptimeText() {
      const s = this.server.uptimeSeconds;
      if (s == null || s === '') return '—';
      const n = Number(s);
      if (Number.isNaN(n) || n < 0) return '—';
      const d = Math.floor(n / 86400);
      const h = Math.floor((n % 86400) / 3600);
      const m = Math.floor((n % 3600) / 60);
      const parts = [];
      if (d) parts.push(`${d} 天`);
      if (h || d) parts.push(`${h} 小时`);
      parts.push(`${m} 分钟`);
      return parts.join(' ');
    },
    cpuRows() {
      const c = this.cpu || {};
      const usage =
        c.usagePercent != null && c.usagePercent !== ''
          ? `${c.usagePercent}%`
          : '—（暂不可用）';
      return [
        { name: '物理核心', value: c.physicalCores != null ? String(c.physicalCores) : '—' },
        { name: '逻辑处理器', value: c.logicalCores != null ? String(c.logicalCores) : '—' },
        { name: '系统 CPU 使用率（采样约 0.3s）', value: usage },
      ];
    },
    memoryRows() {
      const m = this.memory || {};
      return [
        { name: '总内存', value: this.formatBytes(m.totalBytes) },
        { name: '已用', value: this.formatBytes(m.usedBytes) },
        { name: '可用', value: this.formatBytes(m.availableBytes) },
        {
          name: '使用率',
          value: m.usagePercent != null && m.usagePercent !== '' ? `${m.usagePercent}%` : '—',
        },
      ];
    },
  },
  mounted() {
    this.loadMetrics();
  },
  methods: {
    formatBytes(b) {
      if (b == null || b === '' || Number(b) <= 0) return '—';
      const n = Number(b);
      const gb = n / (1024 * 1024 * 1024);
      if (gb >= 1) return `${gb.toFixed(2)} GB`;
      const mb = n / (1024 * 1024);
      if (mb >= 1) return `${mb.toFixed(2)} MB`;
      const kb = n / 1024;
      return `${kb.toFixed(1)} KB`;
    },
    loadMetrics() {
      this.loading = true;
      this.errorMsg = '';
      request
        .get('/administrator/system/metrics')
        .then((res) => {
          if (res.code !== 1 || !res.data) {
            this.errorMsg = res.msg || '加载失败';
            return;
          }
          const d = res.data;
          this.collectedAt = d.collectedAt || '';
          this.server = d.server || {};
          this.cpu = d.cpu || {};
          this.memory = d.memory || {};
          this.jvm = d.jvm || {};
          this.disks = Array.isArray(d.disks) ? d.disks : [];
        })
        .catch((e) => {
          console.error(e);
          this.errorMsg = '网络错误或无权访问（需管理员登录）';
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>

<style scoped>
.system-info {
  padding: 8px;
  max-width: 1280px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.meta {
  color: #909399;
  font-size: 13px;
}
.mb {
  margin-bottom: 12px;
}
.cards-row {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  justify-content: center;
  margin-bottom: 20px;
}
.box-card {
  width: 600px;
  max-width: 100%;
}
.box-card.wide {
  width: 100%;
  max-width: 1240px;
  margin: 0 auto 20px;
}
</style>
