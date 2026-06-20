<template>
    <div class="activity-list-page">
        <div class="page-header">
            <h1 class="page-title">报名活动</h1>
            <p class="page-subtitle">浏览可报名活动，或查看已报名与已结束记录</p>
        </div>

        <div class="toolbar">
            <el-input
                v-model="searchTitle"
                placeholder="搜索活动标题"
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
    name: 'AddActivityPhone',
    data() {
        return {
            filterTabs: [
                { label: '可报名', value: 'available' },
                { label: '已报名', value: 'joined' },
                { label: '已结束', value: 'ended' },
            ],
            searchTitle: '',
            activeTab: 'available',
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
                available: '暂无可报名活动',
                joined: '暂无已报名活动',
                ended: '暂无已结束活动',
            };
            return map[this.activeTab] || '暂无活动';
        },
        hasMore() {
            return this.originalData.length < this.totalItems;
        },
    },
    mounted() {
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
            this.reloadList();
        },
        getStatusTag(row) {
            if (this.activeTab === 'joined') return getActivityCardStatus(row, 'joined');
            if (this.activeTab === 'ended') return getActivityCardStatus(row, 'ended');
            return getActivityCardStatus(row, 'browse');
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

            const requestUrl = this.activeTab === 'available'
                ? `/users/vol?${params}`
                : `users/vol/activity?${params}`;

            return request.get(requestUrl)
                .then((response) => {
                    if (response.code === 1) {
                        this.totalItems = response.data.total;
                        this.originalData = [...this.originalData, ...(response.data.rows || [])];
                        this.applyTabFilter();
                        if (
                            (this.activeTab === 'joined' || this.activeTab === 'ended')
                            && this.tableData.length === 0
                            && this.originalData.length < this.totalItems
                        ) {
                            this.currentPage++;
                            return this.fetchPage();
                        }
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
            const scene = this.activeTab === 'available' ? 'browse' : 'joined';
            this.tableData = filterActivitiesByTab(this.originalData, this.activeTab, scene);
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
            const name = this.activeTab === 'available' ? 'TargetPage' : 'RegisteredActivity';
            this.$router.push({ name, query: { id: row.id } });
        },
    },
};
</script>

<style lang="scss" scoped>
@import '@/styles/volunteer-activity-list.scss';

.activity-list-page {
    @include volunteer-activity-list-page;
}
</style>
