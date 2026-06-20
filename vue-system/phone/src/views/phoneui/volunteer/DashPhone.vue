<template>
    <div class="Box">
        <div>
            <router-view></router-view>
        </div>
        <el-footer>
            <span class="link-container">
                <router-link to="/homePhone" class="RouterLink">
                    <i class="el-icon-house icon"></i>
                    <div class="text">首页</div>
                </router-link>
            </span>
            <span class="link-container">
                <router-link to="/addActivityPhone" class="RouterLink">
                    <i class="el-icon-circle-plus icon"></i>
                    <div class="text">报名活动</div>
                </router-link>
            </span>
            <span class="link-container">
                <router-link to="/artificialVolunteer" class="RouterLink">
                    <i class="el-icon-s-comment icon"></i>
                    <div class="text">AI服务</div>
                </router-link>
            </span>
            <span class="link-container">
                <router-link to="/infoOfUserPhone" class="RouterLink">
                    <i class="el-icon-user-solid icon"></i>
                    <div class="text">个人中心</div>
                </router-link>
            </span>
        </el-footer>
    </div>
</template>

<script>
</script>

<style lang="scss" scoped>
.Box {
    .el-footer{
        display: flex;
        justify-content: space-around;
        align-items: center;
        margin-top: 10px;
        backdrop-filter: blur(10px);
        border-radius: 5px;
        box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
        flex-shrink: 0;
        position: fixed;
        bottom: 0;
        width: 100%;
        padding: 0 10px;
        box-sizing: border-box;
        .RouterLink {
            text-decoration: none;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            text-align: center;
            color: #606266;
            .text {
                font-size: 10px;
                line-height: 1.4;
                white-space: nowrap;
            }
            .icon {
                font-size: 24px;
            }
        }
        .router-link-active {
            color: #409eff;
        }
    }
}

.Box {
    min-height: 100vh;
    padding-bottom: 74px;
    background: var(--vol-bg);
}

.Box .el-footer {
    height: 68px !important;
    margin-top: 0 !important;
    background: rgba(255, 255, 255, 0.96);
    border-top: 1px solid var(--vol-border);
    border-radius: 14px 14px 0 0 !important;
    box-shadow: 0 -5px 16px rgba(22, 119, 166, 0.12) !important;
}

.Box .link-container {
    flex: 1;
}

.Box .RouterLink {
    min-height: 54px;
    border-radius: 10px;
}

.Box .RouterLink .text {
    font-size: 12px !important;
    font-weight: 600;
}

.Box .RouterLink .icon {
    font-size: 24px !important;
}

.Box .router-link-active {
    color: var(--vol-primary);
    background: #e6f4f8;
}
</style>
