<template>
    <div>
        <div class="phone-main">
            <div class="blockOfImage">
            <el-carousel height="150px">
                <el-carousel-item v-for="(item, index) in images" :key="index">
                <a :href="item.link" target="_blank"> <!-- 在新标签页打开链接 -->
                    <img :src="item.url" alt="image" style="width: 100%; height: 100%; object-fit: cover;">
                </a>
                </el-carousel-item>
            </el-carousel>
            </div>
            <el-row>
                <el-col :span="12" v-for="module in modules" :key="module.id">
                    <div class="module-card" @click="navigateToModule(module)">
                        <el-card>
                            <i :class="module.icon"></i>
                            <span>{{ module.name }}</span>
                        </el-card>
                    </div>
                </el-col>
            </el-row>
        </div>
    </div>
</template>

<script>
export default {
    data() {
        return {
            modules: [
                { id: 4, name: '医疗康复', icon: 'icon-doctor', code: 'medical_rehab' },
                { id: 5, name: '健康管理', icon: 'icon-health', code: 'health_manage' },
                { id: 6, name: '清洁整理', icon: 'icon-clear', code: 'cleaning' },
                { id: 7, name: '购物陪同', icon: 'icon-shop', code: 'shopping_companion' },
                { id: 8, name: '问诊陪护', icon: 'icon-chaperonage', code: 'clinic_companion' },
                { id: 9, name: '物品代购', icon: 'icon-shop1', code: 'purchase' },
                { id: 10, name: '其他服务', icon: 'icon-addActivity', code: 'other_service' },
            ],
            // 照片
            images: [
                { url: require('@/assets/common/Carousel1.png'), link: 'https://chinavolunteer.mca.gov.cn/nvsiwebsite/XLFZYFW' },
                { url: require('@/assets/common/Carousel2.png'), link: 'https://mp.weixin.qq.com/s/t7p-uNdgRIKcGD1s5veENA' },
            ],
        };
    },
    methods: {
        navigateToModule(module) {
            // 在这里执行导航操作，比如使用路由导航到模块页面
            console.log('点击了模块卡片', module);
            // 示例：使用 Vue Router 导航到模块详情页面
            this.$router.push({
                path: '/getInfoActivity',
                query: {
                    serviceType: module.code || 'other_service',
                    serviceLabel: module.name || '其他服务',
                }
            });
        },
    }
};
</script>

<style lang='scss' scoped>
.phone-main {
    min-height: calc(100vh - 130px);
    margin: 0;
    padding: 16px 14px 22px;
    background: var(--old-bg);

    .blockOfImage {
        overflow: hidden;
        border: 1px solid var(--old-border);
        border-radius: 14px;
        box-shadow: 0 6px 16px rgba(111, 76, 43, 0.10);
    }

    &::v-deep .el-row {
        margin-top: 8px;
    }

    .module-card {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        min-height: 128px;
        border-radius: 14px;
        cursor: pointer;
        margin: 8px;
        background: var(--old-surface);
        border: 1px solid var(--old-border);
        box-shadow: 0 5px 14px rgba(111, 76, 43, 0.10);

        &::v-deep .el-card {
            width: 100%;
            height: 100%;
            border: none;
            border-radius: 14px;
            background: transparent;
            box-shadow: none;
        }

        &::v-deep .el-card__body {
            min-height: 126px;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 12px 8px;
            box-sizing: border-box;
        }
    }
    .module-card span {
        margin-top: 8px;
        font-size: 18px;
        font-weight: 800;
        line-height: 1.25;
        color: var(--old-text);
        text-align: center;
    }
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

    i {
        width: 48px;
        height: 48px;
        display: inline-block;
        background-size: cover;
        vertical-align: middle;
    }
    i.icon-doctor {
        background-image: url("~@/assets/common/doctor.png");
    }
    i.icon-health {
        background-image: url("~@/assets/common/health.png");
    }
    i.icon-clear {
        background-image: url("~@/assets/common/clear.png");
    }
    i.icon-shop {
        background-image: url("~@/assets/common/shopping.png");
    }
    i.icon-shop1 {
        background-image: url("~@/assets/common/shopping1.png");
    }
    i.icon-chaperonage {
        background-image: url("~@/assets/common/chaperonage.png");
    }
    i.icon-addActivity {
        background-image: url("~@/assets/common/addActivity.png");
    }
}
</style>
