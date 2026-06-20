<template>
    <el-container class="homeBox">
      <el-main class="mainBox">
        <div class="activity-toolbar">
          <el-input
            v-model="searchTitle"
            placeholder="搜索活动标题"
            prefix-icon="el-icon-search"
            clearable
            @keyup.enter.native="handleSearchClick"
          />
          <el-button type="primary" round class="toolbar-search-btn" @click="handleSearchClick">搜索</el-button>
        </div>
        <div class="blockOfImage">
          <el-carousel height="150px">
            <el-carousel-item v-for="(item, index) in images" :key="index">
              <a :href="item.link" target="_blank"> <!-- 在新标签页打开链接 -->
                <img :src="item.url" alt="image" style="width: 100%; height: 100%; object-fit: cover;">
              </a>
            </el-carousel-item>
          </el-carousel>
        </div>
        <div class="noticeBoard" v-loading="noticesLoading">
          <div class="noticeBoard-title">平台公告</div>
          <el-empty v-if="!noticesLoading && notices.length === 0" description="暂无公告"></el-empty>
          <el-collapse v-else v-model="activeNotice" accordion class="notice-collapse">
            <el-collapse-item
              v-for="item in notices"
              :key="item.id"
              :title="noticeItemTitle(item)"
              :name="String(item.id)"
            >
              <p class="notice-content">{{ item.content }}</p>
              <p class="notice-meta">{{ formatNoticeTime(item.createTime) }}</p>
            </el-collapse-item>
          </el-collapse>
        </div>
        <div class="activities">
          <el-main class="activity">
            <ul class="infinite-list" v-infinite-scroll="load" infinite-scroll-disabled="busy" infinite-scroll-distance="5" style="overflow:auto;padding-inline-start:0px">
              <div v-for="(row, index) in tableData" :key="index" @click="handleCardClick(row)">
                <el-card :body-style="{ padding: '0px' }" shadow="always">
                  <div class="cardContent">
                    <img :src="$activityImagePath" class="image">
                    <div class="contentBox">
                      <div style="font-size: 17px;">{{ row.title }}</div>
                      <div style="font-size: 14px;">剩余名额：{{ row.remain }}</div>
                      <div
                        class="volunteer-reward-line"
                        :class="{ 'volunteer-reward-line--zero': formatVolunteerRewardAmount(row) <= 0 }"
                      >
                        答谢（每人）：{{ formatVolunteerRewardAmount(row) }} 时间币
                      </div>
                      <el-progress :percentage="Number(((parseFloat(row.quota) - parseFloat(row.remain)) / parseFloat(row.quota) * 100).toFixed(1))"></el-progress>
                      <div style="display: flex;justify-content: space-between;align-items: center;font-size: 12px;">
                        {{ formatActivityDates(row) }}
                      <el-tag size="mini" v-if="!isBeforeDeadline(row.deadline)" type="danger">报名结束</el-tag>
                      <el-tag size="mini" v-else type="success">报名中</el-tag>
                      </div>
                      <div style="font-size: 12px;">{{ row.address }}</div>
                    </div>
                  </div>
                </el-card>
              </div>
            </ul>
          </el-main>
        </div>
      </el-main>
    </el-container>
</template>

<script>
import request from '@/utils/request';

export default {
  name: 'HomePhone',
  data() {
    return {
      // 照片
      images: [
        { url: require('@/assets/common/Carousel1.png'), link: 'https://chinavolunteer.mca.gov.cn/nvsiwebsite/XLFZYFW' },
        { url: require('@/assets/common/Carousel2.png'), link: 'https://mp.weixin.qq.com/s/t7p-uNdgRIKcGD1s5veENA' },
      ],
      // 卡片
      originalData: [],
      pageSize: 5, // 每页显示的条目数量
      totalItems: 0, // 总条目数量
      currentPage: 1, // 当前页码
      tableData: [], // 表格数据
      searchTitle: '',
      // 无限滚动
      busy: false,
      notices: [],
      noticesLoading: false,
      activeNotice: '',
    }
  },
  mounted() {
    this.fetchNotices();
    this.resetActivityList();
    this.search().catch(() => {});
  },
  methods: {
    resetActivityList() {
      this.tableData = [];
      this.originalData = [];
      this.currentPage = 1;
      this.totalItems = 0;
    },
    handleSearchClick() {
      this.resetActivityList();
      this.search();
    },
    fetchNotices() {
      this.noticesLoading = true;
      request.get('/dashboard/notices?limit=30')
        .then(response => {
          if (response.code === 1 && Array.isArray(response.data)) {
            this.notices = response.data;
          } else {
            this.notices = [];
          }
        })
        .catch(() => {
          this.notices = [];
          this.$message.error('公告加载失败');
        })
        .finally(() => {
          this.noticesLoading = false;
        });
    },
    noticeItemTitle(item) {
      const t = item.title && String(item.title).trim();
      if (t) return t;
      const c = item.content ? String(item.content).trim() : '';
      if (!c) return '公告';
      return c.length > 36 ? `${c.slice(0, 36)}…` : c;
    },
    formatNoticeTime(t) {
      if (!t) return '';
      if (typeof t === 'string') return t;
      if (Array.isArray(t) && t.length >= 3) {
        const pad = (n) => String(n).padStart(2, '0');
        const [y, mo, d, h = 0, mi = 0, se = 0] = t;
        return `${y}-${pad(mo)}-${pad(d)} ${pad(h)}:${pad(mi)}:${pad(se)}`;
      }
      return '';
    },
    formatActivityDates(activity) {
      const message = activity && activity.message ? String(activity.message) : '';
      if (message) {
        try {
          const parsed = JSON.parse(message);
          if (parsed && Array.isArray(parsed.dates) && parsed.dates.length > 0) {
            return parsed.dates
              .map(item => String(item).split('-').pop())
              .map(day => `${parseInt(day, 10)}号`)
              .join(',');
          }
        } catch (error) {
          // Ignore malformed legacy message
        }
      }
      if (!activity || !activity.date) return '日期待定';
      const day = String(activity.date).split('-').pop();
      return `${parseInt(day, 10)}号`;
    },
    formatVolunteerRewardAmount(row) {
      const v = row && row.volunteerReward;
      const n = v === null || v === undefined || v === '' ? 0 : Number(v);
      return Number.isFinite(n) ? n : 0;
    },
    load() {
      if (this.tableData.length >= this.totalItems) {
        
        return;
      }
      if (this.busy) return;
      this.busy = true;

      this.search()
        .then(() => {
          this.busy = false;
        })
        .catch(() => {
          this.busy = false;
        });
    },
    search() {
      return new Promise((resolve, reject) => {
        // 创建 URLSearchParams 对象
        const params = new URLSearchParams();
        // 添加搜索条件到 URLSearchParams 对象中
        params.append('pageSize', this.pageSize);
        params.append('page', this.currentPage);
        const title = (this.searchTitle || '').trim();
        if (title) params.append('title', title);
        // 将 URLSearchParams 对象转换为查询字符串
        const queryString = params.toString();

        // 发起请求时将查询字符串添加到URL中
        request.get(`/users/vol?${queryString}`)
          .then(response => {
            if (response.code === 1) {
              this.totalItems = response.data.total;
              this.originalData = response.data.rows;
              this.tableData = [...this.tableData, ...this.originalData];
              this.currentPage++;
              resolve(this.tableData);
            } else {
              const hint =
                response.msg === 'ACCESS_RESTRICTED'
                  ? '当前账号无权访问志愿者活动列表（请使用志愿者账号登录）'
                  : response.msg;
              this.$message.error(hint);
              reject(new Error(response.msg || 'REQUEST_FAILED'));
            }
          })
          .catch(error => {
              console.error('获取数据失败:', error);
              reject(error);
          });
      });
    },
    handleCardClick(row) {
      // 在发送路由跳转时将数据作为查询参数传递
      this.$router.push({ 
          name: 'TargetPage', 
          query: { 
              id: row.id
          } 
      });
    },
    // 判断是否在报名截止日期之前
    isBeforeDeadline(deadline) {
      // 将截止日期字符串转换为日期对象
      const deadlineDate = new Date(deadline);
      // 获取当前时间
      const currentDate = new Date();
      // 如果当前时间早于截止日期，则返回 true，否则返回 false
      return currentDate < deadlineDate;
    },
  }
}
</script>

<style lang="scss" scoped>
.homeBox{
  display: flex;
  flex-direction: column;
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */
  .Title{
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin: 5px;
    height: 40px !important;
    backdrop-filter: blur(10px);
    border-radius: 20px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
  }
  .mainBox{
    width: 100%;
    .activity-toolbar{
      display: flex;
      align-items: center;
      gap: 10px;
      margin: 10px 10px 0;
      box-sizing: border-box;
      .el-input{
        flex: 1;
        min-width: 0;
      }
      .toolbar-search-btn{
        flex-shrink: 0;
      }
    }
    // 走马灯
    .blockOfImage{
      .el-carousel__item h3 {
        color: #475669;
        font-size: 14px;
        opacity: 0.75;
        line-height: 150px;
        margin: 0;
      }

      .el-carousel__item:nth-child(2n) {
        background-color: #99a9bf;
      }
      
      .el-carousel__item:nth-child(2n+1) {
        background-color: #d3dce6;
      }
    }
    .noticeBoard{
      margin-top: 10px;
      backdrop-filter: blur(10px);
      border-radius: 8px;
      box-shadow: 0 0 10px rgba(0, 0, 0, 0.12);
      padding: 12px;
      min-height: 72px;
      .noticeBoard-title{
        font-size: 15px;
        font-weight: 600;
        margin-bottom: 8px;
        color: #303133;
      }
      .notice-collapse{
        border: none;
      }
      .notice-content{
        white-space: pre-wrap;
        word-break: break-word;
        margin: 0 0 8px;
        font-size: 14px;
        line-height: 1.5;
        color: #606266;
      }
      .notice-meta{
        margin: 0;
        font-size: 12px;
        color: #909399;
      }
    }
    .activities{
      .activity{
        display: flex;
        flex-direction: column;
        justify-content: center; /* 水平居中 */
        align-items: center; /* 垂直居中 */
        padding: 0px;
        .el-card{
          display: flex;
          padding: 5px;
          min-height: 156px;
          align-items: center;
          margin-bottom: 15px;
          .cardContent{
            display: flex;
            justify-content: space-between;
            align-items: center;
            .image {
              width: 40%;
              display: block;
              border-radius: 10px;
              box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
            }
            .contentBox {
              padding: 8px;
              width: 60%;
              .volunteer-reward-line {
                font-size: 13px;
                margin-top: 4px;
                color: #67c23a;
                font-weight: 500;
              }
              .volunteer-reward-line--zero {
                color: #909399;
                font-weight: 400;
              }
            }
          }
        } 
      }
    }
  }
}

.homeBox {
  min-height: calc(100vh - 122px);
  background: var(--vol-bg);
}

.homeBox .mainBox {
  padding-bottom: 12px;
}

.homeBox .activity-toolbar ::v-deep .el-input__inner {
  border-color: var(--vol-border);
  border-radius: 10px;
}

.homeBox .activity-toolbar .toolbar-search-btn {
  background: var(--vol-primary);
  border-color: var(--vol-primary);
}

.homeBox .blockOfImage {
  margin: 10px;
  overflow: hidden;
  border: 1px solid var(--vol-border);
  border-radius: 14px;
  box-shadow: 0 5px 16px rgba(22, 119, 166, 0.10);
}

.homeBox .noticeBoard {
  margin: 10px !important;
  border: 1px solid var(--vol-border);
  border-radius: 14px !important;
  background: var(--vol-surface);
  box-shadow: 0 5px 16px rgba(22, 119, 166, 0.10) !important;
}

.homeBox .noticeBoard .noticeBoard-title {
  color: var(--vol-primary-strong) !important;
}

.homeBox .activities .activity .el-card {
  border: 1px solid var(--vol-border);
  border-radius: 14px;
  background: var(--vol-surface);
  box-shadow: 0 5px 16px rgba(22, 119, 166, 0.10);
}

.homeBox .activities .activity .contentBox {
  color: #243746;
}

.homeBox ::v-deep .el-progress-bar__inner {
  background: linear-gradient(90deg, var(--vol-accent), var(--vol-primary));
}
</style>
