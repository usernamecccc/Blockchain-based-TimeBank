<template>
    <div class="activity-old-page">
        <el-dialog
            :visible.sync="dialogVisible"
            title="发布新活动"
            width="88%"
            :append-to-body="true"
        >
            <el-result icon="warning" title="是否确认" :subTitle="publishFeeHint">
                <template slot="extra">
                    <el-button type="primary" size="medium" round @click="addActivity">开始填写</el-button>
                </template>
            </el-result>
        </el-dialog>

        <div class="page-header">
            <div>
                <h1 class="page-title">我发布的活动</h1>
                <p class="page-subtitle">管理报名、审核与志愿者答谢</p>
            </div>
            <el-button type="primary" round class="publish-btn" @click="addOpen">
                <i class="el-icon-plus"></i> 发布
            </el-button>
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
                v-for="tab in statusTabs"
                :key="tab.value"
                type="text"
                class="status-tab"
                :class="{ active: status === tab.value }"
                @click="switchStatus(tab.value)"
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
                            <div class="card-title-row">
                                <span class="card-title">{{ row.title }}</span>
                            </div>
                            <div class="card-tags">
                                <el-tag size="mini" :type="getAuditStatusTag(row.status).type">
                                    {{ getAuditStatusTag(row.status).label }}
                                </el-tag>
                                <el-tag size="mini" type="info" effect="plain">
                                    {{ formatServiceTypeLabel(row.serviceType) }}
                                </el-tag>
                                <el-tag size="mini" :type="getRegistrationTag(row).type">
                                    {{ getRegistrationTag(row).label }}
                                </el-tag>
                            </div>

                            <div class="card-meta">
                                <span>名额 {{ filledQuota(row) }}/{{ row.quota }}</span>
                                <span class="reward-text" :class="{ 'reward-text--zero': formatVolunteerRewardAmount(row) <= 0 }">
                                    答谢 {{ formatVolunteerRewardAmount(row) }} 币/人
                                </span>
                            </div>

                            <el-progress
                                :percentage="quotaPercent(row)"
                                :stroke-width="8"
                                :show-text="false"
                                class="card-progress"
                            />

                            <div class="card-footer">
                                <div class="card-info">
                                    <div><i class="el-icon-date"></i> {{ formatActivityDates(row) }}</div>
                                    <div class="card-address"><i class="el-icon-location-outline"></i> {{ row.address }}</div>
                                </div>
                                <el-button
                                    type="danger"
                                    plain
                                    round
                                    size="mini"
                                    class="delete-btn"
                                    @click.stop="confirmDelete(row)"
                                >删除</el-button>
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
    formatServiceTypeLabel,
    getAuditStatusTag,
    getRegistrationTag,
    quotaPercent,
} from '@/utils/oldActivity';

export default {
    name: 'ActivityOld',
    data() {
        return {
            dialogVisible: false,
            listLoading: false,
            originalData: [],
            pageSize: 8,
            totalItems: 0,
            currentPage: 1,
            tableData: [],
            searchTitle: '',
            status: 2,
            publishFeeHint: '加载规则中…',
            busy: false,
            statusTabs: [
                { label: '待审核', value: 1 },
                { label: '已通过', value: 2 },
                { label: '进行中', value: 3 },
                { label: '已拒绝', value: 4 },
                { label: '已过期', value: 5 },
            ],
        };
    },
    computed: {
        emptyHint() {
            const map = {
                1: '暂无待审核活动',
                2: '暂无已通过活动',
                3: '暂无进行中活动',
                4: '暂无被拒绝活动',
                5: '暂无已过期活动',
            };
            return map[this.status] || '暂无活动，点击右上角发布';
        },
        hasMore() {
            return this.tableData.length < this.totalItems;
        },
    },
    mounted() {
        this.fetchPublishFee();
        this.reloadList();
    },
    methods: {
        formatActivityDates,
        formatVolunteerRewardAmount,
        formatServiceTypeLabel,
        getAuditStatusTag,
        getRegistrationTag,
        quotaPercent,
        filledQuota(row) {
            const quota = Number(row && row.quota);
            const remain = Number(row && row.remain);
            if (!Number.isFinite(quota)) return 0;
            const filled = quota - (Number.isFinite(remain) ? remain : 0);
            return Math.max(0, filled);
        },
        fetchPublishFee() {
            request.get('/info/publishActivityFee')
                .then((res) => {
                    if (res.code === 1 && res.data) {
                        if (res.data.deductEnabled) {
                            this.publishFeeHint = `确认后将进入填写流程；发布成功时链上将扣除 ${res.data.cost} 时间币`;
                        } else {
                            this.publishFeeHint = '确认后将进入填写流程（当前未启用链上扣费）';
                        }
                    } else {
                        this.publishFeeHint = '确认后将进入填写流程';
                    }
                })
                .catch(() => {
                    this.publishFeeHint = '确认后将进入填写流程';
                });
        },
        reloadList() {
            this.currentPage = 1;
            this.totalItems = 0;
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
            params.append('status', String(this.status));
            const title = (this.searchTitle || '').trim();
            if (title) params.append('title', title);

            return request.get(`/users/old?${params}`)
                .then((response) => {
                    if (response.code === 1) {
                        this.totalItems = response.data.total;
                        this.tableData = [...this.tableData, ...(response.data.rows || [])];
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
        switchStatus(status) {
            if (this.status === status) return;
            this.status = status;
            this.reloadList();
        },
        addOpen() {
            this.dialogVisible = true;
        },
        addActivity() {
            this.dialogVisible = false;
            this.$router.push({ name: 'GetInfoActivity' });
        },
        handleCardClick(row) {
            this.$router.push({
                name: 'IdActivityOld',
                query: { id: row.id },
            });
        },
        confirmDelete(row) {
            this.$confirm(`确定删除活动「${row.title}」吗？此操作不可恢复。`, '删除活动', {
                confirmButtonText: '删除',
                cancelButtonText: '取消',
                type: 'warning',
            }).then(() => {
                this.deleteActivity(row.id);
            }).catch(() => {});
        },
        deleteActivity(id) {
            request.delete(`/users/old/${id}`)
                .then((response) => {
                    if (response.code === 1) {
                        this.$message.success(response.msg || '删除成功');
                        this.reloadList();
                    } else {
                        this.$message.error(response.msg);
                    }
                })
                .catch((error) => {
                    console.error('删除失败:', error);
                    this.$message.error('删除失败，请稍后重试');
                });
        },
    },
};
</script>

<style lang="scss" scoped>
.activity-old-page {
    min-height: calc(100vh - 130px);
    padding: 12px 12px 24px;
    background: var(--old-bg);
    color: var(--old-text);
    box-sizing: border-box;
}

.page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;
    padding: 4px 2px 0;
}

.page-title {
    margin: 0;
    font-size: 22px;
    font-weight: 700;
    color: var(--old-primary-strong);
    line-height: 1.3;
}

.page-subtitle {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--old-muted);
}

.publish-btn {
    flex-shrink: 0;
    min-height: 40px;
    padding: 0 16px;
    background: var(--old-primary);
    border-color: var(--old-primary);
}

.toolbar {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;

    .el-input {
        flex: 1;
        min-width: 0;
    }
}

.toolbar ::v-deep .el-input__inner {
    min-height: 44px;
    border-color: var(--old-border);
    border-radius: 12px;
    font-size: 15px;
}

.toolbar ::v-deep .el-button--primary {
    min-height: 44px;
    background: var(--old-primary);
    border-color: var(--old-primary);
}

.status-tabs {
    display: flex;
    gap: 8px;
    margin-bottom: 14px;
    padding: 4px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
}

.status-tab {
    flex-shrink: 0;
    min-width: 72px;
    height: 34px;
    padding: 0 14px;
    border: 1px solid transparent;
    border-radius: 17px;
    font-weight: 600;
    color: var(--old-muted);
}

.status-tab.active {
    color: var(--old-primary);
    border-color: var(--old-primary);
    background: var(--old-surface);
}

.list-panel {
    min-height: 200px;
}

.list-empty {
    padding: 48px 0;
}

.load-more-wrap {
    display: flex;
    justify-content: center;
    padding: 8px 0 16px;
}

.load-more-wrap ::v-deep .el-button {
    min-width: 140px;
    color: var(--old-primary);
    border-color: var(--old-primary);
}

.list-end-hint {
    margin: 8px 0 16px;
    text-align: center;
    font-size: 13px;
    color: var(--old-muted);
}

.activity-list {
    list-style: none;
    margin: 0;
    padding: 0;
}

.activity-card {
    display: flex;
    gap: 12px;
    margin-bottom: 14px;
    padding: 12px;
    border: 1px solid var(--old-border);
    border-radius: 16px;
    background: var(--old-surface);
    box-shadow: 0 5px 16px rgba(111, 76, 43, 0.08);
    cursor: pointer;
    transition: transform 0.15s ease, box-shadow 0.15s ease;

    &:active {
        transform: scale(0.99);
    }
}

.card-image {
    width: 88px;
    height: 88px;
    flex-shrink: 0;
    border-radius: 12px;
    object-fit: cover;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.card-body {
    flex: 1;
    min-width: 0;
}

.card-title-row {
    margin-bottom: 6px;
}

.card-title {
    font-size: 16px;
    font-weight: 700;
    line-height: 1.4;
    color: var(--old-text);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.card-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 8px;
}

.card-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: var(--old-muted);
    margin-bottom: 6px;
}

.reward-text {
    color: #67c23a;
    font-weight: 600;
    white-space: nowrap;
}

.reward-text--zero {
    color: var(--old-muted);
    font-weight: 400;
}

.card-progress {
    margin-bottom: 8px;
}

.card-progress ::v-deep .el-progress-bar__inner {
    background: linear-gradient(90deg, #e8a87c, var(--old-primary));
}

.card-footer {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 8px;
}

.card-info {
    flex: 1;
    min-width: 0;
    font-size: 12px;
    color: var(--old-muted);
    line-height: 1.5;

    i {
        margin-right: 2px;
    }
}

.card-address {
    margin-top: 2px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.delete-btn {
    flex-shrink: 0;
}
</style>
