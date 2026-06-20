<template>
    <div class="activity-list-page with-footer">
        <div class="page-header">
            <h1 class="page-title">我报名的活动</h1>
            <p class="page-subtitle">查看签到进度、服务状态与时间币答谢</p>
        </div>

        <div class="toolbar">
            <el-input
                v-model="searchTitle"
                placeholder="搜索已报名活动"
                prefix-icon="el-icon-search"
                clearable
                @keyup.enter.native="handleSearch"
            />
            <el-button type="primary" round @click="handleSearch">搜索</el-button>
        </div>

        <div class="status-tabs">
            <el-button
                v-for="tab in filterTabs"
                :key="tab.value"
                type="text"
                class="status-tab"
                :class="{ active: activeTab === tab.value }"
                @click="switchTab(tab.value)"
            >{{ tab.label }}</el-button>
        </div>

        <div class="list-panel" v-loading="listLoading">
            <el-empty
                v-if="!listLoading && tableData.length === 0"
                :description="emptyHint"
                class="list-empty"
            />

            <div v-else class="list-content">
                <ul class="activity-list">
                    <li
                        v-for="row in tableData"
                        :key="row.id"
                        class="activity-card"
                        @click="handleCardClick(row)"
                    >
                        <img :src="$activityImagePath" class="card-image" alt="">
                        <div class="card-body">
                            <div class="card-title">{{ row.title }}</div>
                            <div class="card-tags">
                                <el-tag size="mini" :type="getStatusTag(row).type">
                                    {{ getStatusTag(row).label }}
                                </el-tag>
                                <el-tag v-if="Number(row.volSign) === 1" size="mini" type="success">已签到</el-tag>
                                <el-tag v-else size="mini" type="info" effect="plain">未签到</el-tag>
                            </div>
                            <div class="card-meta">
                                <span>名额 {{ filledQuota(row) }}/{{ row.quota }}</span>
                                <span
                                    class="reward-text"
                                    :class="{ 'reward-text--zero': formatVolunteerRewardAmount(row) <= 0 }"
                                >
                                    答谢 {{ formatVolunteerRewardAmount(row) }} 币/人
                                </span>
                            </div>
                            <el-progress
                                :percentage="quotaPercent(row)"
                                :stroke-width="8"
                                :show-text="false"
                                class="card-progress"
                            />
                            <div class="card-info">
                                <div><i class="el-icon-date"></i> {{ formatActivityDates(row) }}</div>
                                <div class="card-address"><i class="el-icon-location-outline"></i> {{ row.address }}</div>
                            </div>
                        </div>
                    </li>
                </ul>
                <div v-if="hasMore" class="load-more-wrap">
                    <el-button round :loading="busy" @click="loadMore">
                        {{ busy ? '加载中…' : '加载更多' }}
                    </el-button>
                </div>
                <p v-else-if="tableData.length > 0" class="list-end-hint">已显示全部活动</p>
            </div>
        </div>

        <el-footer class="operations">
            <span>
                <router-link to="/homePhone" class="RouterLink">
                    <i class="el-icon-house"></i>首页
                </router-link>
            </span>
            <span>
                <router-link to="/addActivityPhone" class="RouterLink">
                    <i class="el-icon-circle-plus"></i>报名活动
                </router-link>
            </span>
            <span>
                <router-link to="/infoOfUserPhone" class="RouterLink">
                    <i class="el-icon-user-solid"></i>个人中心
                </router-link>
            </span>
        </el-footer>
    </div>
</template>

<script>
import request from '@/utils/request';
import {
    formatActivityDates,
    formatVolunteerRewardAmount,
    getActivityCardStatus,
    filterActivitiesByTab,
    quotaPercent,
    filledQuota,
} from '@/utils/volunteerActivity';

export default {
    name: 'ActivityOfUser',
    data() {
        return {
            filterTabs: [
                { label: '已报名', value: 'joined' },
                { label: '进行中', value: 'ongoing' },
                { label: '已结束', value: 'ended' },
            ],
            activeTab: 'joined',
            searchTitle: '',
            originalData: [],
            pageSize: 8,
            totalItems: 0,
            currentPage: 1,
            tableData: [],
            listLoading: false,
            busy: false,
        };
    },
    computed: {
        emptyHint() {
            const map = {
                joined: '暂无已报名活动',
                ongoing: '暂无进行中的活动',
                ended: '暂无已结束活动',
            };
            return map[this.activeTab] || '暂无数据';
        },
        hasMore() {
            return this.originalData.length < this.totalItems;
        },
    },
    mounted() {
        this.reloadList();
    },
    activated() {
        this.reloadList();
    },
    methods: {
        formatActivityDates,
        formatVolunteerRewardAmount,
        quotaPercent,
        filledQuota,
        switchTab(tab) {
            if (this.activeTab === tab) return;
            this.activeTab = tab;
            this.applyTabFilter();
        },
        getStatusTag(row) {
            const scene = this.activeTab === 'ended' ? 'ended' : 'joined';
            return getActivityCardStatus(row, scene);
        },
        reloadList() {
            this.currentPage = 1;
            this.totalItems = 0;
            this.originalData = [];
            this.tableData = [];
            this.busy = false;
            this.listLoading = true;
            return this.fetchPage()
                .then(() => {
                    this.currentPage = 2;
                })
                .finally(() => {
                    this.listLoading = false;
                });
        },
        fetchPage() {
            const params = new URLSearchParams();
            params.append('pageSize', String(this.pageSize));
            params.append('page', String(this.currentPage));
            const title = (this.searchTitle || '').trim();
            if (title) params.append('title', title);

            return request.get(`users/vol/activity?${params}`)
                .then((response) => {
                    if (response.code === 1) {
                        this.totalItems = response.data.total;
                        this.originalData = [...this.originalData, ...(response.data.rows || [])];
                        this.applyTabFilter();
                        return this.tableData;
                    }
                    this.$message.error(response.msg);
                    return Promise.reject(response.msg);
                })
                .catch((error) => {
                    console.error('获取数据失败:', error);
                    return Promise.reject(error);
                });
        },
        applyTabFilter() {
            this.tableData = filterActivitiesByTab(this.originalData, this.activeTab, 'joined');
        },
        loadMore() {
            if (!this.hasMore || this.busy || this.listLoading) return;
            this.busy = true;
            this.fetchPage()
                .then(() => {
                    this.currentPage++;
                })
                .finally(() => {
                    this.busy = false;
                });
        },
        handleSearch() {
            this.reloadList();
        },
        handleCardClick(row) {
            this.$router.push({
                name: 'RegisteredActivity',
                query: { id: row.id },
            });
        },
    },
};
</script>

<style lang="scss" scoped>
@import '@/styles/volunteer-activity-list.scss';

.activity-list-page {
    @include volunteer-activity-list-page;

    &.with-footer {
        min-height: calc(100vh - 48px);
        padding-bottom: 78px;
    }
}

.operations {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
    backdrop-filter: blur(10px);
    background: rgba(255, 255, 255, 0.96);
    border-radius: 5px;
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
    flex-shrink: 0;
    position: fixed;
    bottom: 0;
    left: 0;
    width: 100%;
    z-index: 10;

    .RouterLink {
        text-decoration: none;
        color: var(--vol-primary);
        font-size: 13px;
    }
}
</style>
