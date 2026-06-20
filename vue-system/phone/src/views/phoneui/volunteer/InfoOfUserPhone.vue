<template>
    <div class="infoBox">
        <el-header>
            <el-button type="text" @click="redirectToInfo" style="margin-left: auto;">
                <i class="el-icon-setting" style="font-size:x-large;"></i>
            </el-button>
        </el-header>
        <div class="mainBox">
            <div class="titleBOX">
                <div class="titleBox">
                    <div>
                        <img v-if="squareUrl" class="avatar1" :src="squareUrl">
                        <span v-else class="username1">
                            <div style="font-size: 30px;">
                                {{ infoData.username?.charAt(0) }}
                            </div>
                        </span>
                    </div>
                    <div>
                        {{infoData.username}}
                    </div>
                    <div class="user-id-text">用户 ID：{{ infoData.id != null && infoData.id !== '' ? infoData.id : '—' }}</div>
                    <div class="coin-total-row">
                        <span class="coin-total-label">时间币总数</span>
                        <span class="coin-total-value">{{ coinBalanceDisplay }}</span>
                    </div>
                </div>
            </div>
            <div class="contentBox">
                <el-menu style="width: 100%;">
                    <router-link to="/coinInfo" class="RouterLink">
                        <el-menu-item index="1">
                            <div>
                                <i class="el-icon-coin"></i>
                                <el-button style="background: none; border: none; padding: 0; cursor: pointer;">时间币详情</el-button>
                            </div>
                            <div>
                                <i class="el-icon-arrow-right"></i>
                            </div>
                        </el-menu-item>
                    </router-link>
                    <div @click="queryActivity">
                        <el-menu-item index="2">
                            <div>
                                <i class="el-icon-s-flag"></i>
                                <el-button style="background: none; border: none; padding: 0; cursor: pointer;">我参加的活动</el-button>
                            </div>
                            <div>
                                <i class="el-icon-arrow-right"></i>
                            </div>
                        </el-menu-item>
                    </div>
                    
                </el-menu>
            </div>
        </div>
    </div>
</template>

<script>
import router from '@/router';
import request from '@/utils/request';

export default {
    name: 'InfoOfUserPhone',
    data() {
        return{
            infoData: {},
            squareUrl: '',
            coinBalance: null,
            coinBalanceLoading: true,
            coinChainReady: true,
        }
    },
    computed: {
        coinBalanceDisplay() {
            if (this.coinBalanceLoading) return '…';
            if (this.coinBalance === null || this.coinBalance === undefined) return '—';
            return this.coinBalance;
        },
    },
    created() {
        this.search();
        this.fetchCoinBalance();
    },
    methods: {
        redirectToInfo() {
            router.push('infoPhone');
        },
        search() {    
            request.get(`/info`)
                .then(response => {
                if (response.code === 1) {
                    this.infoData = response.data;
                    this.squareUrl = response.data.image;
                } else {
                    this.$message.error(response.msg);
                }
                })
                .catch(error => {
                    console.error('获取数据失败:', error);
                });
        },
        queryActivity() {
            this.$router.push('activityOfUser');
        },
        fetchCoinBalance() {
            this.coinBalanceLoading = true;
            request.get('/info/coinBalance')
                .then(response => {
                    if (response.code === 1 && response.data) {
                        this.coinBalance = response.data.balance != null ? String(response.data.balance) : '0';
                        this.coinChainReady = response.data.chainReady !== false;
                        if (!this.coinChainReady && response.data.reason) {
                            console.warn('链未就绪:', response.data.reason);
                        }
                    } else {
                        this.coinBalance = '—';
                    }
                })
                .catch(() => {
                    this.coinBalance = '—';
                })
                .finally(() => {
                    this.coinBalanceLoading = false;
                });
        },
    }
}
</script>

<style lang="scss" scoped>
.infoBox {
    .el-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    .mainBox{
        display: flex;
        align-items: center;
        flex-direction: column;
        height: 100%;
        .titleBOX {
            width: 90%;
            height: 140px;
            background-color: #fff;
            border-radius: 5px;
            box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
            display: flex;
            justify-content: center;
            align-items: center;
            flex-shrink: 0; /* 防止底部内容被压缩 */
            .titleBox{
                display: flex;
                flex-direction: column;
                justify-content: center;
                align-items: center;
                flex-shrink: 0; /* 防止底部内容被压缩 */
                margin-right: 0;
                .avatar1 {
                    display: inline-block;
                    width: 60px;
                    height: 60px;
                    border-radius: 12px;
                    background-color: #d9d9d9;
                    line-height: 48px;
                    text-align: center;
                }
                .username1 {
                    display: inline-block;
                    width: 60px;
                    height: 60px;
                    text-align: center;
                    line-height: 60px;
                    border-radius: 50%;
                    background: #04c9be;
                    color: #fff;
                    margin-right: 4px;
                }
                .user-id-text {
                    margin-top: 4px;
                    font-size: 13px;
                    color: #909399;
                }
                .coin-total-row {
                    display: flex;
                    flex-direction: row;
                    align-items: baseline;
                    justify-content: center;
                    gap: 10px;
                    margin-top: 10px;
                    width: 100%;
                    flex-wrap: nowrap;
                }
                .coin-total-label {
                    font-size: 14px;
                    color: #606266;
                    white-space: nowrap;
                }
                .coin-total-value {
                    font-size: 22px;
                    font-weight: 700;
                    color: var(--vol-primary-strong);
                    line-height: 1;
                    letter-spacing: 0.02em;
                }
            }
        }
        .contentBox{
            width: 100%;
            height: auto;
            display: flex;
            align-items: center;
            flex-direction: column;
            flex-shrink: 0; /* 防止底部内容被压缩 */
            margin: 10px;
            .el-menu{
                width: 100%;
                .el-menu-item{
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
            }
        }
    }
}
</style>