<template>
    <div class="Box">
        <div>
            <router-view></router-view>
        </div>
        <el-footer>
            <span class="link-container" style="margin-left: 35px;">
                <router-link to="/serverOld" class="RouterLink">
                    <i class="el-icon-menu icon"></i>
                    <div class="text">服务中心</div>
                </router-link>
            </span>
            <span class="link-container">
                <router-link to="/artificialOld" class="RouterLink">
                    <i class="el-icon-s-comment icon"></i>
                    <div class="text">AI服务</div>
                </router-link>
            </span>
            <span class="link-container" style="margin-right: 35px;">
                <router-link to="/homeOld" class="RouterLink">
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
        justify-content: space-between;
        align-items: center;
        margin-top: 10px;
        backdrop-filter: blur(10px);
        border-radius: 5px;
        box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
        flex-shrink: 0; /* 防止底部内容被压缩 */
        position: fixed; /* 将底部组件固定在页面底部 */
        bottom: 0;
        width: 100%; /* 设置宽度为 100% */
        .RouterLink {
            text-decoration: none;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            text-align: center;
            .text {
                font-size: 10px; /* 设置文字的大小 */
            }
            .icon {
                font-size: 24px; /* 设置图标的大小 */
            }
        }
    }
}

.Box {
    min-height: 100vh;
    padding-bottom: 82px;
    background: var(--old-bg);
}

.Box .el-footer {
    height: 76px !important;
    margin-top: 0 !important;
    padding: 0 12px !important;
    box-sizing: border-box;
    background: rgba(255, 255, 255, 0.96);
    border-top: 1px solid var(--old-border);
    border-radius: 16px 16px 0 0 !important;
    box-shadow: 0 -6px 18px rgba(93, 63, 33, 0.14) !important;
}

.Box .link-container {
    flex: 1;
    margin: 0 !important;
}

.Box .RouterLink {
    min-height: 60px;
    color: var(--old-muted);
    border-radius: 12px;
}

.Box .RouterLink .text {
    margin-top: 3px;
    font-size: 14px !important;
    font-weight: 700;
    line-height: 1.25;
}

.Box .RouterLink .icon {
    font-size: 27px !important;
}

.Box .router-link-active {
    color: var(--old-primary);
    background: #e8f4f8;
}
</style>
