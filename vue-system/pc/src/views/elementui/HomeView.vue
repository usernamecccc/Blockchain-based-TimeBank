<template>
  <div class="dashboard">
    <div class="container">
      <!-- 左侧内容 -->
      <div
        class="left"
        v-loading="dashboardLoading"
        element-loading-text="加载统计数据..."
        element-loading-background="rgba(255,255,255,0.6)"
      >
        <div class="panel">
          <!-- 个人信息 -->
          <div class="user-info">
            <img v-if="avatar" class="avatar" :src="avatar" alt="">
            <span v-else class="username">{{ name?.charAt(0) }}</span>
            <div class="company-info">
              <div class="depart">{{ name }} ｜ {{ company }}-{{ departmentName }}</div>
            </div>
          </div>
          <!-- 代办 -->
          <div class="todo-list">
            <div class="todo-item">
              <span>时间币总个数</span>
              <!-- 起始值 终点值 动画时间 -->
              <count-to
                :start-val="0"
                :end-val="homeData.coinTotal"
                :duration="1000"
              />
            </div>
            <div class="todo-item">
              <span>注册人数</span>
              <count-to
                :start-val="0"
                :end-val="homeData.registTotal"
                :duration="1000"
              />
            </div>
            <div class="todo-item">
              <span>活动个数</span>
              <count-to
                :start-val="0"
                :end-val="homeData.ActivityTotal"
                :duration="1000"
              />
            </div>
            <div class="todo-item">
              <span>志愿者人数</span>
              <count-to
                :start-val="0"
                :end-val="homeData.volunteerTotal"
                :duration="1000"
              />
            </div>
            <div class="todo-item">
              <span>老人人数</span>
              <count-to
                :start-val="0"
                :end-val="homeData.oldManTotal"
                :duration="1000"
              />
            </div>
            <div class="todo-item">
              <span>总参与人次</span>
              <count-to
                :start-val="0"
                :end-val="homeData.totalEngagement"
                :duration="1000"
              />
            </div>
          </div>
        </div>
        <!-- 快捷入口 -->
        <div class="panel">
          <div class="panel-title">快捷入口</div>
          <div class="quick-entry">
            <div class="entry-item" role="button" tabindex="0" @click="goQuickEntry('/usersView')">
              <div class="entry-icon role" />
              <span>用户管理</span>
            </div>
            <div class="entry-item" role="button" tabindex="0" @click="goQuickEntry('/coinView')">
              <div class="entry-icon salary" />
              <span>时间币管理</span>
            </div>
            <div class="entry-item" role="button" tabindex="0" @click="goQuickEntry('/adminView')">
              <div class="entry-icon departMent" />
              <span>活动管理</span>
            </div>
            <div class="entry-item" role="button" tabindex="0" @click="goQuickEntry('/noticeManageView')">
              <div class="entry-icon approval" />
              <span>公告管理</span>
            </div>
            <div class="entry-item" role="button" tabindex="0" @click="goQuickEntry(null)">
              <div class="entry-icon support" />
              <span>人工服务</span>
            </div>
            <div class="entry-item" role="button" tabindex="0" @click="goQuickEntry('/systemView')">
              <div class="entry-icon home" />
              <span>系统管理</span>
            </div>
          </div>
        </div>
        <!-- 图表数据 -->
        <div class="panel">
          <div class="panel-title">用户角色数据</div>
          <div class="chart-container">
            <div class="chart-info">
              <div class="info-main">
                <span>注册人数</span>
                <!-- homeData: {} -->
                <count-to
                  :start-val="0"
                  :end-val="homeData.registTotal"
                  :duration="1000"
                />
              </div>
              <div class="info-list">
                <div class="info-list-item">
                  <span>志愿者(人)</span>
                  <count-to
                    :start-val="0"
                    :end-val="homeData.volunteerTotal"
                    :duration="1000"
                  />
                </div>
                <div class="info-list-item">
                  <span>老人(人)</span>
                  <count-to
                    :start-val="0"
                    :end-val="homeData.oldManTotal"
                    :duration="1000"
                  />
                </div>
                <div class="info-list-item">
                  <span>管理员(人)</span>
                  <count-to
                    :start-val="0"
                    :end-val="homeData.AdministratorTotal"
                    :duration="1000"
                  />
                </div>
              </div>
            </div>
            <div class="chart">
              <div id="myChart0" style="width: 600px; height: 400px;"></div>
            </div>
          </div>
        </div>
        <!-- 图表数据 -->
        <div class="panel">
          <div class="panel-title">活动服务数据</div>
          <div class="chart-container">
            <div class="chart-info">
              <div class="info-main">
                <span>活动个数</span>
                <count-to
                  :start-val="0"
                  :end-val="homeData.ActivityTotal"
                  :duration="1000"
                />
              </div>
              <div class="info-list">
                <div class="info-list-item">
                  <span>待审核(个)</span>
                  <count-to
                    :start-val="0"
                    :end-val="homeData.processingActivityTotal"
                    :duration="1000"
                  />
                </div>
                <div class="info-list-item">
                  <span>审核通过(个)</span>
                  <count-to
                    :start-val="0"
                    :end-val="homeData.approvedActivityTotal"
                    :duration="1000"
                  />
                </div>
                <div class="info-list-item">
                  <span>进行中(个)</span>
                  <count-to
                    :start-val="0"
                    :end-val="homeData.continueActivityTotal"
                    :duration="1000"
                  />
                </div>
                <div class="info-list-item">
                  <span>拒绝进行(个)</span>
                  <count-to
                    :start-val="0"
                    :end-val="homeData.rejectedActivityTotal"
                    :duration="1000"
                  />
                </div>
                <div class="info-list-item">
                  <span>活动过期(个)</span>
                  <count-to
                    :start-val="0"
                    :end-val="homeData.expiredActivityTotal"
                    :duration="1000"
                  />
                </div>
              </div>
            </div>
            <div class="chart">
              <div
                class="HelloWorld echart-box"
                id="myChart1"
                :style="{ width: '520px', height: '400px',background:'#ffffff'}"
              ></div>
            </div>
          </div>
        </div>
      </div>
      <!-- 右侧内容 -->
      <div class="right">
        <!-- 活动日历：点选日期后下方展示当日活动 -->
        <div class="panel calendar-panel">
          <div class="panel-title">活动日历</div>
          <div class="calendar-wrap">
            <el-calendar v-model="calendarValue" />
          </div>
          <div
            class="calendar-day-list"
            v-loading="calendarDayLoading"
            element-loading-text="加载当日活动…">
            <div class="calendar-day-list-title">{{ calendarDateLabel }} · 当日活动</div>
            <div v-if="!calendarDayLoading && !dayActivities.length" class="calendar-day-empty">当日暂无活动</div>
            <div v-else class="calendar-day-items">
              <div
                v-for="(row, idx) in dayActivities"
                :key="row.id"
                class="calendar-day-item"
                role="button"
                tabindex="0"
                @click="goActivityManage"
                @keyup.enter="goActivityManage">
                <div class="calendar-day-item-index" aria-hidden="true">{{ idx + 1 }}</div>
                <div class="calendar-day-item-body">
                  <div class="calendar-day-item-head">
                    <span class="calendar-day-item-title">{{ row.title }}</span>
                    <el-tag size="mini" :type="dayActivityTagType(row.status)" class="calendar-day-item-tag">
                      {{ dayActivityStatusText(row.status) }}
                    </el-tag>
                  </div>
                  <div class="calendar-day-item-row">
                    <i class="el-icon-time" />
                    <span>{{ row.begin }} — {{ row.end }}</span>
                  </div>
                  <div class="calendar-day-item-row calendar-day-item-address">
                    <i class="el-icon-location-outline" />
                    <span>{{ row.address }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- 通知公告（主页最多 3 条） -->
        <div class="panel notice-board-panel">
          <div class="panel-title">通知公告</div>
          <div class="notice-board-sub">最新 3 条</div>
          <div v-if="!list.length" class="notice-board-empty">暂无公告</div>
          <div v-else class="notice-board-list">
            <div
              v-for="(item, idx) in list"
              :key="item.id || idx"
              class="notice-board-card">
              <div class="notice-board-card-index" aria-hidden="true">{{ idx + 1 }}</div>
              <div class="notice-board-card-body">
                <div v-if="item.title" class="notice-board-card-title">{{ item.title }}</div>
                <div class="notice-board-card-content">{{ item.content }}</div>
                <div class="notice-board-card-time">
                  <i class="el-icon-time" />
                  <span>{{ item.createTime }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import CountTo from 'vue-count-to'
import * as echarts from "echarts";
import request from '@/utils/request';

const emptyHomeData = () => ({
  coinTotal: 0,
  registTotal: 0,
  ActivityTotal: 0,
  interfaceAccessTotal: 0,
  volunteerTotal: 0,
  oldManTotal: 0,
  AdministratorTotal: 0,
  processingActivityTotal: 0,
  approvedActivityTotal: 0,
  siginActivityTotal: 0,
  continueActivityTotal: 0,
  endActivityTotal: 0,
  rejectedActivityTotal: 0,
  expiredActivityTotal: 0,
  totalEngagement: 0,
});

export default {
  name: 'HomeView',
  components: {
    CountTo,
  },
  data() {
    return {
      homeData: emptyHomeData(),
      list: [],
      name: '',
      avatar: '',
      company: '时间银行管理平台',
      departmentName: '',
      barChart: null,
      pieChart: null,
      pieSeriesData: [],
      roleBarValues: [0, 0, 0],
      dashboardLoading: false,
      calendarValue: new Date(),
      dayActivities: [],
      calendarDayLoading: false,
    }
  },
  computed: {
    calendarDateLabel() {
      const d = this.calendarValue;
      if (!d || !(d instanceof Date) || isNaN(d.getTime())) return '';
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${y}年${m}月${day}日`;
    },
  },
  watch: {
    calendarValue: {
      handler() {
        this.fetchCalendarDayActivities();
      },
      immediate: true,
    },
  },
  mounted() {
    this._resizeCharts = () => {
      if (this.barChart) this.barChart.resize();
      if (this.pieChart) this.pieChart.resize();
    };
    window.addEventListener('resize', this._resizeCharts);
    this.$nextTick(() => {
      const el0 = document.getElementById('myChart0');
      const el1 = document.getElementById('myChart1');
      if (el0) this.barChart = echarts.init(el0);
      if (el1) this.pieChart = echarts.init(el1);
      this.renderBarChart();
      this.drawLine();
      this.loadDashboard();
    });
  },
  beforeDestroy() {
    window.removeEventListener('resize', this._resizeCharts);
    if (this.barChart) {
      this.barChart.dispose();
      this.barChart = null;
    }
    if (this.pieChart) {
      this.pieChart.dispose();
      this.pieChart = null;
    }
  },
  methods: {
    roleLabel(role) {
      const r = Number(role);
      if (r === 1) return '老人';
      if (r === 2) return '志愿者';
      if (r === 3) return '管理员';
      return '用户';
    },
    goQuickEntry(path) {
      if (!path) {
        this.$message.info('人工服务暂未开放');
        return;
      }
      if (this.$route.path !== path) {
        this.$router.push(path).catch(err => {
          if (err.name !== 'NavigationDuplicated') console.error(err);
        });
      }
    },
    goActivityManage() {
      this.goQuickEntry('/adminView');
    },
    formatDateToYMD(val) {
      if (!val) return '';
      const date = val instanceof Date ? val : new Date(val);
      if (isNaN(date.getTime())) return '';
      const y = date.getFullYear();
      const m = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${y}-${m}-${day}`;
    },
    fetchCalendarDayActivities() {
      const dateStr = this.formatDateToYMD(this.calendarValue);
      if (!dateStr) return;
      this.calendarDayLoading = true;
      const params = new URLSearchParams();
      params.append('page', '1');
      params.append('pageSize', '3');
      params.append('date', dateStr);
      request
        .get(`/administrator?${params.toString()}`)
        .then(res => {
          if (res.code === 1 && res.data && Array.isArray(res.data.rows)) {
            this.dayActivities = res.data.rows;
          } else {
            this.dayActivities = [];
            if (res.code !== 1 && res.msg) this.$message.error(res.msg);
          }
        })
        .catch(() => {
          this.dayActivities = [];
        })
        .finally(() => {
          this.calendarDayLoading = false;
        });
    },
    dayActivityStatusText(status) {
      const s = Number(status);
      const map = { 1: '待审核', 2: '审核通过', 3: '进行中', 4: '拒绝进行', 5: '活动过期' };
      return map[s] || '—';
    },
    dayActivityTagType(status) {
      const s = Number(status);
      if (s === 1) return 'warning';
      if (s === 2 || s === 3) return 'success';
      if (s === 4) return 'danger';
      if (s === 5) return 'info';
      return '';
    },
    async loadDashboard() {
      this.dashboardLoading = true;
      try {
        await Promise.all([this.fetchDashboardStats(), this.fetchUserProfile(), this.fetchNotices()]);
        this.renderBarChart();
        this.drawLine();
        this.$nextTick(() => this._resizeCharts && this._resizeCharts());
      } finally {
        this.dashboardLoading = false;
      }
    },
    fetchNotices() {
      return request
        .get('/dashboard/notices?limit=3')
        .then(res => {
          if (res.code === 1 && Array.isArray(res.data)) {
            this.list = res.data.slice(0, 3);
          }
        })
        .catch(err => console.error('获取公告失败:', err));
    },
    fetchUserProfile() {
      return request.get('/info')
        .then(res => {
          if (res.code !== 1 || !res.data) return;
          const u = res.data;
          this.name = u.name || u.username || '';
          this.departmentName = this.roleLabel(u.role);
          if (u.image) {
            this.avatar = `${request.defaults.baseURL}/image/${u.image}`;
          } else {
            this.avatar = '';
          }
        })
        .catch(err => console.error('获取个人信息失败:', err));
    },
    fetchDashboardStats() {
      return request.get('/dashboard/stats')
        .then(res => {
          if (res.code !== 1 || !res.data) return;
          const { userStats, activityStats } = res.data;
          const us = userStats || {};
          const elder = Number(us.elder) || 0;
          const volunteer = Number(us.volunteer) || 0;
          const admin = Number(us.admin) || 0;
          const act = activityStats || {};
          const dist = act.statusDistribution || {};
          const pending = Number(dist.pending) || 0;
          const approved = Number(dist.approved) || 0;
          const ongoing = Number(dist.ongoing) || 0;
          const rejected = Number(dist.rejected) || 0;
          const expired = Number(dist.expired) || 0;
          const activitySum = pending + approved + ongoing + rejected + expired;

          this.roleBarValues = [elder, volunteer, admin];
          this.pieSeriesData = [
            { value: pending, name: '待审核' },
            { value: approved, name: '审核通过' },
            { value: ongoing, name: '进行中' },
            { value: rejected, name: '拒绝进行' },
            { value: expired, name: '活动过期' },
          ];

          this.homeData = {
            ...emptyHomeData(),
            registTotal: elder + volunteer + admin,
            volunteerTotal: volunteer,
            oldManTotal: elder,
            AdministratorTotal: admin,
            ActivityTotal: activitySum,
            processingActivityTotal: pending,
            approvedActivityTotal: approved,
            continueActivityTotal: ongoing,
            rejectedActivityTotal: rejected,
            expiredActivityTotal: expired,
            totalEngagement: Number(act.totalEngagement) || 0,
          };
        })
        .catch(err => {
          console.error('获取仪表盘统计失败:', err);
          this.$message.error('仪表盘数据加载失败，请检查网络或稍后刷新');
        });
    },
    renderBarChart() {
      if (!this.barChart) return;
      const [elder, volunteer, admin] = this.roleBarValues;
      const options = {
        title: {
          text: '用户角色人数分布',
          left: 'center',
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        xAxis: {
          type: 'category',
          data: ['老人', '志愿者', '管理员']
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '人数',
            type: 'bar',
            data: [elder, volunteer, admin]
          }
        ]
      };
      this.barChart.setOption(options);
    },
    drawLine() {
      if (!this.pieChart) return;
      const options = {
        title: {
          text: '活动状态分析',
          left: 'center',
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b} : {c} ({d}%)',
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          data: ['待审核', '审核通过', '进行中', '拒绝进行', '活动过期'],
        },
        series: [
          {
            name: '活动状态',
            type: 'pie',
            radius: '55%',
            center: ['50%', '60%'],
            data: this.pieSeriesData.length
              ? this.pieSeriesData
              : [
                  { value: 0, name: '待审核' },
                  { value: 0, name: '审核通过' },
                  { value: 0, name: '进行中' },
                  { value: 0, name: '拒绝进行' },
                  { value: 0, name: '活动过期' },
                ],
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)',
              },
            },
          },
        ],
      };
      this.pieChart.setOption(options);
    },
  },
}
</script>

<style scoped lang="scss">
.dashboard {
  background: #f5f6f8;
  width: 100%;
  min-height: calc(100vh - 80px);

  ::v-deep .el-calendar-day {
  height:  40px;
 }
 ::v-deep .el-calendar-table__row td,::v-deep .el-calendar-table tr td:first-child, ::v-deep .el-calendar-table__row td.prev{
  border:none;
 }

 ::v-deep .el-calendar-table td.is-selected .text{
   background: #409eff;
   color: #fff;
   border-radius: 50%;
 }
  .container {
    display: flex;
    .right {
      width: 40%;
      .panel {
        margin-left: 8px;
      }
      :nth-child(1) {
        margin-top: 0;
      }
    }
    .left {
      flex: 1;
      :nth-child(1) {
        margin-top: 0;
      }
    }
    .panel {
      background-color: #fff;

      margin-top: 8px;
      padding: 20px;
      .panel-title {
        font-size: 16px;
        color: #383c4e;
        font-weight: 500;
      }
      // 用户信息样式
      .user-info {
        display: flex;
        .avatar {
          width: 48px;
          height: 48px;
          border-radius: 12px;
          background-color: #d9d9d9;
          line-height: 48px;
          text-align: center;
        }
         .username {
           width: 30px;
           height: 30px;
           text-align: center;
           line-height: 30px;
           border-radius: 50%;
           background: #04c9be;
           color: #fff;
           margin-right: 4px;
         }
        .company-info {
          margin-left: 10px;
          min-height: 48px;
          display: flex;
          flex-direction: column;
          justify-content: center;
          .depart {
            font-size: 15px;
            color: #383c4e;
            font-weight: 500;
            line-height: 1.4;
          }
        }
      }
      // 代办样式
      .todo-list {
        margin-top: 10px;
        display: flex;
        flex-wrap: wrap;
        .todo-item {
          width: 18%;
          height: 90px;
          display: flex;
          flex-direction: column;
          padding: 10px;
          justify-content: space-around;
          :nth-child(1) {
            color: #697086;
            font-size: 14px;
          }
          :nth-child(2) {
            color: #383c4e;
            font-size: 30px;
            font-weight: 500;
          }
        }
      }
      // 快捷入口
      .quick-entry {
        margin-top: 16px;
        display: flex;
        .entry-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          margin-left: 60px;
          cursor: pointer;
          &:nth-child(1) {
            margin-left: 0px;
          }
          .entry-icon {
            width: 40px;
            height: 40px;
            border-radius: 10px;
            background: #f5f6f8;
            background-size: cover;
            &.approval {
              background-image: url('~@/assets/common/approval.png');
            }
             &.social {
              background-image: url('~@/assets/common/social.png');
            }
             &.salary {
              background-image: url('~@/assets/common/salary.png');
            }
            &.role {
              background-image: url('~@/assets/common/role.png');
            }
            &.bpm {
              background-image: url('~@/assets/common/bpm.png');
            }
            &.departMent {
              background-image: url('~@/assets/common/departMent.png');
            }
            &.support {
              background-image: url('~@/assets/common/support.png');
            }
            &.home {
              background-image: url('~@/assets/common/home.png');
            }
          }
          span {
            color: #697086;
            font-size: 14px;
            margin-top: 8px;
          }
        }
      }
      // 图表数据
      .chart-container {
        display: flex;
        .chart-info {
         width: 240px;
          margin-top: 10px;
          .info-main {
            padding: 10px;
            display: flex;
            flex-direction: column;
            :nth-child(1) {
              font-size: 14px;
              color: #697086;
            }
            :nth-child(2) {
              margin-top: 10px;
              font-size: 30px;
              color: #04c9be;
              font-weight: 500;
            }
          }
          .info-list {
            background: #f5f6f8;
            border-radius: 4px;
            padding: 12px 15px;
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            .info-list-item {
              width: 50%;
              margin-top: 10px;
              display: flex;
              flex-direction: column;

              :nth-child(1) {
                font-size: 14px;
                color: #697086;
              }
              :nth-child(2) {
                margin-top: 10px;
                font-size: 30px;
                color: #383c4e;
                font-weight: 500;
              }
            }
          }
        }
        .chart {
          flex:1
        }
      }
      /* 与模板中 class="panel calendar-panel" 同一元素，须用 & 合并选择器，否则样式不生效 */
      &.calendar-panel {
        .calendar-wrap {
          margin-top: 8px;
        }
        .calendar-day-list {
          margin-top: 16px;
          padding: 14px 12px 12px;
          border-top: 1px solid #ebeef5;
          min-height: 80px;
          background: #fafbfc;
          border-radius: 0 0 8px 8px;
        }
        .calendar-day-list-title {
          font-size: 13px;
          color: #606266;
          font-weight: 500;
          margin-bottom: 14px;
          padding-bottom: 10px;
          border-bottom: 1px dashed #dcdfe6;
        }
        .calendar-day-empty {
          font-size: 14px;
          color: #909399;
          text-align: center;
          padding: 20px 8px;
        }
        .calendar-day-items {
          display: flex;
          flex-direction: column;
          gap: 12px;
        }
        .calendar-day-item {
          display: flex;
          align-items: flex-start;
          gap: 12px;
          padding: 14px 14px 14px 12px;
          background: #fff;
          border: 1px solid #e4e7ed;
          border-radius: 10px;
          border-left: 4px solid #409eff;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
          cursor: pointer;
          transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
          &:hover {
            border-color: #c6e2ff;
            box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12);
            transform: translateY(-1px);
          }
          &:focus {
            outline: none;
            box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.35);
          }
        }
        .calendar-day-item-index {
          flex-shrink: 0;
          width: 26px;
          height: 26px;
          line-height: 26px;
          text-align: center;
          border-radius: 8px;
          background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
          color: #409eff;
          font-size: 13px;
          font-weight: 600;
        }
        .calendar-day-item-body {
          flex: 1;
          min-width: 0;
        }
        .calendar-day-item-head {
          display: flex;
          align-items: flex-start;
          justify-content: space-between;
          gap: 10px;
          margin-bottom: 10px;
        }
        .calendar-day-item-title {
          font-size: 14px;
          font-weight: 600;
          color: #303133;
          line-height: 1.4;
          word-break: break-word;
        }
        .calendar-day-item-tag {
          flex-shrink: 0;
        }
        .calendar-day-item-row {
          display: flex;
          align-items: flex-start;
          gap: 6px;
          font-size: 12px;
          color: #606266;
          line-height: 1.5;
          margin-top: 6px;
          &:first-of-type {
            margin-top: 0;
          }
          i {
            flex-shrink: 0;
            margin-top: 2px;
            color: #909399;
            font-size: 14px;
          }
          span {
            word-break: break-word;
          }
        }
        .calendar-day-item-address span {
          color: #606266;
        }
      }
      &.notice-board-panel {
        .notice-board-sub {
          font-size: 12px;
          color: #909399;
          margin-top: 4px;
        }
        .notice-board-empty {
          margin-top: 16px;
          padding: 20px 8px;
          text-align: center;
          font-size: 14px;
          color: #909399;
        }
        .notice-board-list {
          margin-top: 14px;
          display: flex;
          flex-direction: column;
          gap: 10px;
        }
        .notice-board-card {
          display: flex;
          align-items: flex-start;
          gap: 10px;
          padding: 12px 12px 12px 10px;
          background: #fff;
          border: 1px solid #e4e7ed;
          border-radius: 10px;
          border-left: 3px solid #67c23a;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
        }
        .notice-board-card-index {
          flex-shrink: 0;
          width: 22px;
          height: 22px;
          line-height: 22px;
          text-align: center;
          border-radius: 6px;
          background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%);
          color: #67c23a;
          font-size: 12px;
          font-weight: 600;
        }
        .notice-board-card-body {
          flex: 1;
          min-width: 0;
        }
        .notice-board-card-title {
          font-size: 14px;
          font-weight: 600;
          color: #303133;
          line-height: 1.4;
          margin-bottom: 6px;
          word-break: break-word;
        }
        .notice-board-card-content {
          font-size: 13px;
          color: #606266;
          line-height: 1.55;
          word-break: break-word;
          display: -webkit-box;
          -webkit-line-clamp: 4;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .notice-board-card-time {
          display: flex;
          align-items: center;
          gap: 6px;
          margin-top: 8px;
          font-size: 12px;
          color: #909399;
          i {
            font-size: 13px;
          }
        }
      }
    }
  }
}
</style>
