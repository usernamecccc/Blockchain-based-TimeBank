<template>
    <div class="pageBox">
      <img :src="$activityImagePath" class="image">
      <div class="content">
        <div>
            {{ form.title }}
        </div>
        <el-divider content-position="center">报名截止</el-divider>
        <div style="width: 100%; display: inline-block; margin-bottom: 10px;" v-if="deadline">
            <el-statistic
              format="DD天HH小时mm分钟ss秒"
              :value="deadline"
              time-indices
            >
            </el-statistic>
        </div>
        <el-button round style="width: 80%;" v-if="showClosedBanner">报名结束</el-button>
        <el-button type="primary" round style="width: 80%;" @click="signUp" v-if="showJoinButton">点击报名</el-button>
        <el-button type="warning" round plain style="width: 80%;" @click="cancelEnrollment" v-if="showCancelButton">取消报名</el-button>
        <div v-if="alreadyEnrolled && enrollmentChecked" style="margin-top:10px;font-size:13px;color:#67c23a;">您已报名该活动</div>
        <div class="ai-prepare-inline">
          <el-button size="mini" type="text" :loading="aiPrepareLoading" @click="generatePreparation">
            {{ aiPrepare ? '刷新服务准备建议' : '生成服务准备建议' }}
          </el-button>
          <el-collapse v-if="aiPrepare" v-model="aiPreparePanels" accordion>
            <el-collapse-item name="prepare">
              <template slot="title">
                <span class="ai-prepare-title">AI准备：{{ aiPrepare.summary }}</span>
              </template>
              <div class="ai-prepare-grid">
                <div v-if="aiPrepare.checklist && aiPrepare.checklist.length">
                  <div class="ai-prepare-section-title">准备清单</div>
                  <div v-for="item in aiPrepare.checklist" :key="item" class="ai-prepare-item">· {{ item }}</div>
                </div>
                <div v-if="aiPrepare.communicationTips && aiPrepare.communicationTips.length">
                  <div class="ai-prepare-section-title">沟通建议</div>
                  <div v-for="item in aiPrepare.communicationTips" :key="item" class="ai-prepare-item">· {{ item }}</div>
                </div>
                <div v-if="aiPrepare.riskTips && aiPrepare.riskTips.length" class="warning">
                  <div class="ai-prepare-section-title">风险提醒</div>
                  <div v-for="item in aiPrepare.riskTips" :key="item" class="ai-prepare-item">· {{ item }}</div>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
      <div class="content">
        <el-form ref="form" :model="form" label-width="100px" style="width: 100%;">
            <el-form-item label="活动标题">
                <el-input v-model="form.title" readonly prefix-icon="el-icon-edit"></el-input>
            </el-form-item>
            <el-form-item label="活动名额">
                <el-input v-model="form.quota" readonly prefix-icon="el-icon-user"></el-input>
            </el-form-item>
            <el-form-item label="剩余名额">
                <el-input v-model="form.remain" readonly prefix-icon="el-icon-sell"></el-input>
            </el-form-item>
            <el-form-item label="报名截止时间">
                <div class="block">
                    <el-date-picker
                        v-model="form.deadline"
                        type="datetime"
                        placeholder="选择日期时间"
                        align="right"
                        :picker-options="pickerOptionsofform"
                        readonly
                    >
                    </el-date-picker>
                </div>
            </el-form-item>
            <el-form-item label="活动日期">
                <el-input :value="formatActivityDates(form)" readonly prefix-icon="el-icon-date"></el-input>
            </el-form-item>
            <el-form-item label="活动开始时间">
                <el-input v-model="form.begin" readonly prefix-icon="el-icon-time"></el-input>
            </el-form-item>
            <el-form-item label="活动结束时间">
                <el-input v-model="form.end" readonly prefix-icon="el-icon-time"></el-input>
            </el-form-item>
            <el-form-item label="活动地址">
                <el-input v-model="form.address" readonly prefix-icon="el-icon-location"></el-input>
            </el-form-item>
            <el-form-item label="发布人电话">
                <el-input v-model="form.phone" readonly prefix-icon="el-icon-phone"></el-input>
            </el-form-item>
            <el-form-item label="活动描述">
                <el-input type="textarea" v-model="form.description" readonly></el-input>
            </el-form-item>
            <el-form-item label="活动状态">
                <el-input v-model="statusLabel" readonly prefix-icon="el-icon-info"></el-input>
            </el-form-item>
        </el-form>
      </div>
    </div>
</template>
  
<script>
import request from '@/utils/request';

export default {
    name: 'TargetPage',
    data() {
        return {
            form: {},
            id: null,
            pageSize: 1,
            currentPage: 1,
            /** 当前登录志愿者是否已在中间表报名该活动 */
            alreadyEnrolled: false,
            /** 是否已请求过报名状态（避免未返回前误显按钮） */
            enrollmentChecked: false,
            aiPrepare: null,
            aiPrepareLoading: false,
            aiPreparePanels: '',
            // 日期表
            pickerOptionsofsearch: {
                disabledDate(time) {
                return time.getTime() > Date.now();
                },
                shortcuts: [{
                text: '今天',
                onClick(picker) {
                    picker.$emit('pick', new Date());
                }
                }, {
                text: '昨天',
                onClick(picker) {
                    const date = new Date();
                    date.setTime(date.getTime() - 3600 * 1000 * 24);
                    picker.$emit('pick', date);
                }
                }, {
                text: '一周前',
                onClick(picker) {
                    const date = new Date();
                    date.setTime(date.getTime() - 3600 * 1000 * 24 * 7);
                    picker.$emit('pick', date);
                }
                }]
            },
            // 选择器
            options: [
                { value: 1, label: '待审核' },
                { value: 2, label: '审核通过' },
                { value: 3, label: '进行中' },
                { value: 4, label: '拒绝进行' },
                { value: 5, label: '活动过期' },
            ],
            pickerOptionsofform: {
                shortcuts: [{
                text: '今天',
                onClick(picker) {
                    picker.$emit('pick', new Date());
                }
                }, {
                text: '昨天',
                onClick(picker) {
                    const date = new Date();
                    date.setTime(date.getTime() - 3600 * 1000 * 24);
                    picker.$emit('pick', date);
                }
                }, {
                text: '一周前',
                onClick(picker) {
                    const date = new Date();
                    date.setTime(date.getTime() - 3600 * 1000 * 24 * 7);
                    picker.$emit('pick', date);
                }
                }]
            },
            
        };
    },
    created() {
        // 从查询参数中获取数据
        this.id = parseInt(this.$route.query.id);
        this.search();
    },
    computed: {
        deadline() {
            // 确保 form.deadline 有值并且是有效的日期格式
            if (this.form.deadline && !isNaN(Date.parse(this.form.deadline))) {
                // 计算 deadline 并将结果保存到一个计算属性中
                return new Date(this.form.deadline);
            } else {
                return null;
            }
        },
        statusLabel() {
            const selectedOption = this.options.find(option => option.value === this.form.status);
            return selectedOption ? selectedOption.label : '未知状态';
        },
        /** 报名尚未截止 */
        deadlineOpen() {
            if (!this.form.deadline) return false;
            const deadlineDate = new Date(this.form.deadline);
            if (Number.isNaN(deadlineDate.getTime())) return false;
            return deadlineDate > new Date();
        },
        showJoinButton() {
            return this.enrollmentChecked && this.deadlineOpen && !this.alreadyEnrolled;
        },
        showCancelButton() {
            return this.enrollmentChecked && this.deadlineOpen && this.alreadyEnrolled;
        },
        showClosedBanner() {
            return this.enrollmentChecked && !this.deadlineOpen;
        }
    },
    methods: {
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
        search() {
            // 创建 URLSearchParams 对象
            const params = new URLSearchParams();
            // 添加搜索条件到 URLSearchParams 对象中
            params.append('pageSize', this.pageSize);
            params.append('page', this.currentPage);
            params.append('id', this.id);
            // 将 URLSearchParams 对象转换为查询字符串
            const queryString = params.toString();

            // 发起请求时将查询字符串添加到URL中
            request.get(`users/volold/activity?${queryString}`)
                .then(response => {
                if (response.code === 1) {
                    this.form = response.data;
                    this.fetchEnrollmentStatus();
                } else {
                    this.$message.error(response.msg);
                }
                })
                .catch(error => {
                    console.error('获取数据失败:', error);
                });
        },
        /**
         * 后端：已报名则 GET /users/vol/sign 返回 code=1 与 VolActivity；未报名返回非 1（如「查询不到该活动」）。
         */
        fetchEnrollmentStatus() {
            this.enrollmentChecked = false;
            const params = new URLSearchParams();
            params.append('activityId', this.id);
            request.get(`users/vol/sign?${params.toString()}`)
                .then((response) => {
                    this.alreadyEnrolled = response.code === 1 && response.data != null;
                })
                .catch(() => {
                    this.alreadyEnrolled = false;
                })
                .finally(() => {
                    this.enrollmentChecked = true;
                });
        },
        signUp() {
            const data = {
                id: this.id,
            };
            request.put(`/users/vol`,data)
                .then(response => {
                if (response.code === 1) {
                    this.$message.success("报名成功");
                    this.alreadyEnrolled = true;
                    this.search();
                    setTimeout(() => {
                        this.$router.push({ 
                            name: 'HomePhone',
                        });
                    }, 1000);
                } else {
                    this.$message.error(response.msg);
                }
                })
                .catch(error => {
                    console.error('报名失败:', error);
                });
        },
        cancelEnrollment() {
            const data = { id: this.id };
            request.put(`users/vol/cancel`, data)
                .then((response) => {
                    if (response.code === 1) {
                        this.$message.success(response.msg || '已取消报名');
                        this.alreadyEnrolled = false;
                        this.search();
                    } else {
                        this.$message.error(response.msg);
                    }
                })
                .catch((error) => {
                    console.error('取消报名失败:', error);
                });
        },
        generatePreparation() {
            if (!this.form || !this.form.id) {
                this.$message.warning('活动信息还未加载完成');
                return;
            }
            this.aiPrepareLoading = true;
            request.post('/users/vol/ai/activity-prepare', { activity: this.form }, { timeout: 150000 })
                .then((response) => {
                    if (response.code === 1 && response.data) {
                        this.aiPrepare = response.data;
                        this.aiPreparePanels = 'prepare';
                    } else {
                        this.$message.error(response.msg || 'AI准备建议生成失败');
                    }
                })
                .catch(() => {
                    this.$message.error('AI服务暂时不可用');
                })
                .finally(() => {
                    this.aiPrepareLoading = false;
                });
        }
    }
}
</script>

<style lang="scss" scoped>
.pageBox {
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
  padding: 10px;
  .image {
    width: 95%;
    height: auto;
  }
  .content{
    margin: 10px;
    backdrop-filter: blur(10px);
    border-radius: 10px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
    height: auto;
    width: 90%;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    padding: 20px;
  }

  .ai-prepare-inline {
    width: 100%;
    margin-top: 8px;
    border-top: 1px solid #ebeef5;
    padding-top: 6px;
  }

  .ai-prepare-inline ::v-deep .el-collapse {
    width: 100%;
    border-top: none;
    border-bottom: none;
  }

  .ai-prepare-inline ::v-deep .el-collapse-item__header {
    height: auto;
    min-height: 36px;
    line-height: 1.35;
    padding: 6px 0;
    border-bottom: none;
  }

  .ai-prepare-title {
    display: block;
    max-width: 95%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
    font-weight: 600;
    color: #303133;
  }

  .ai-prepare-grid {
    display: grid;
    gap: 8px;
    width: 100%;
    text-align: left;
    font-size: 13px;
    line-height: 1.45;
  }

  .ai-prepare-item {
    color: #606266;
  }

  .warning .ai-prepare-item {
    color: #e6a23c;
  }

  .ai-prepare-section-title {
    font-weight: 600;
    margin-bottom: 4px;
  }

}
</style>
