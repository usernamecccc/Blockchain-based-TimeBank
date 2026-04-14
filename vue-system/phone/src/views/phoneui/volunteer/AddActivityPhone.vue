<template>
    <div class="addActivityBox">
        <el-header class="searchBox">
            <el-input type="text" v-model="searchTitle" placeholder="请输入活动名称" prefix-icon="el-icon-search" style="margin-right: 10px;"></el-input>
            <el-button round @click = "search4" style="width: auto;">搜索</el-button>
        </el-header>
        <el-container class="mainBox">
            <el-header class="groupBox">
                <el-button type="text" @click="search1">可报名</el-button>
                <el-divider direction="vertical"></el-divider>
                <el-button type="text" @click="search2">已报名</el-button>
                <el-divider direction="vertical"></el-divider>
                <el-button type="text" @click="search3">已结束</el-button>
            </el-header>
            <el-main class="activity">
                <ul class="infinite-list" v-infinite-scroll="load" infinite-scroll-disabled="busy" infinite-scroll-distance="5" style="overflow:auto;padding-inline-start:0px">
                    <div v-for="(row, index) in tableData" :key="index" @click="handleCardClick(row)">
                        <el-card :body-style="{ padding: '0px' }" shadow="always">
                        <div class="cardContent">
                            <img :src="$activityImagePath" class="image">
                            <div class="contentBox">
                            <div style="font-size: 17px;">{{ row.title }}</div>
                            <div style="font-size: 14px;">剩余名额：{{ row.quota }}</div>
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
        </el-container>
    </div>
</template>

<script>
import request from '@/utils/request';

export default {
    name: 'AddActivityPhone',
    data() {
        return {
            // 搜索数据
            searchTitle: '',
            // 当前筛选: available(可报名) | joined(已报名) | ended(已结束)
            activeTab: 'available',
            // 卡片
            originalData: [],
            pageSize: 5, // 每页显示的条目数量
            totalItems: 0, // 总条目数量
            currentPage: 1, // 当前页码
            tableData: [], // 表格数据
            // 无限滚动
            busy: false,
        }
    },
    mounted() {
        // 初始化时计算当前页的数据
        this.search();
    },
    methods: {
        parseDateTime(value) {
            if (!value) return null;
            if (value instanceof Date) return isNaN(value.getTime()) ? null : value;
            const normalized = String(value).replace(' ', 'T');
            const parsed = new Date(normalized);
            return isNaN(parsed.getTime()) ? null : parsed;
        },
        parseActivityTime(date, time) {
            if (!date || !time) return null;
            const normalized = `${date}T${String(time).split('.')[0]}`;
            const parsed = new Date(normalized);
            return isNaN(parsed.getTime()) ? null : parsed;
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
        load() {
            if (this.originalData.length >= this.totalItems) {
                
                return;
            }
            if (this.busy) return;
            this.busy = true;

            // 调用你的search方法来获取新的数据
            this.search().finally(() => {
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
                if (this.searchTitle) {
                    params.append('title', this.searchTitle);
                }
                // 将 URLSearchParams 对象转换为查询字符串
                const queryString = params.toString();
                const requestUrl = this.activeTab === 'joined'
                    ? `users/vol/activity?${queryString}`
                    : `/users/vol?${queryString}`;
                // 发起请求时将查询字符串添加到URL中
                request.get(requestUrl)
                    .then(response => {
                        if (response.code === 1) {
                            this.totalItems = response.data.total;
                            this.originalData = [...this.originalData, ...response.data.rows];
                            this.applyTabFilter();
                            this.currentPage++;
                            // 将新的数据作为Promise的结果返回
                            resolve(this.tableData);
                        } else {
                            this.$message.error(response.msg);
                            reject(response.msg);
                        }
                    })
                    .catch(error => {
                        console.error('获取数据失败:', error);
                        reject(error);
                    });
            });   
        },
        applyTabFilter() {
            const now = new Date();
            this.tableData = this.originalData.filter(row => {
                const deadline = this.parseDateTime(row.deadline);
                const activityEnd = this.parseActivityTime(row.date, row.end);

                if (this.activeTab === 'available') {
                    // 解析失败时按“可报名”保留，避免整页被误过滤为空
                    if (!deadline) return true;
                    return deadline > now;
                }
                if (this.activeTab === 'joined') {
                    if (!activityEnd) return true;
                    return now <= activityEnd;
                }
                if (!activityEnd) return !!deadline && deadline <= now;
                return now > activityEnd;
            });
        },
        resetAndSearch() {
            this.currentPage = 1;
            this.totalItems = 0;
            this.originalData = [];
            this.tableData = [];
            this.search();
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
        search1() {
            this.activeTab = 'available';
            this.resetAndSearch();
        },
        search2() {
            this.activeTab = 'joined';
            this.resetAndSearch();
        },
        search3() {
            this.activeTab = 'ended';
            this.resetAndSearch();
        },
        search4() {
            this.resetAndSearch();
        }
    }
}
</script>

<style lang="scss" scoped>
.addActivityBox {
    .searchBox{
        margin-top: 5px;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    .mainBox{
        .groupBox{
          display: flex;
          align-items: center;
          margin-left: 10px;
        }
        .activity{
          display: flex;
          flex-direction: column;
          justify-content: center; /* 水平居中 */
          align-items: center; /* 垂直居中 */
          padding: 10px;
          .el-card{
            display: flex;
            padding: 5px;
            height: 140px;
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
              }
            }
            
          } 
        }
    }
}
</style>