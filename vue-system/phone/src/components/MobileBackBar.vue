<template>
  <header class="mobile-back-bar">
    <el-button type="text" class="mobile-back-bar__btn" native-type="button" @click="onBack">
      <i class="el-icon-arrow-left"></i>
      <span class="mobile-back-bar__label">返回</span>
    </el-button>
    <span class="mobile-back-bar__title" v-if="title">{{ title }}</span>
    <span class="mobile-back-bar__flex" aria-hidden="true"></span>
  </header>
</template>

<script>
export default {
  name: 'MobileBackBar',
  props: {
    title: {
      type: String,
      default: ''
    },
    fallbackPath: {
      type: String,
      required: true
    }
  },
  methods: {
    onBack() {
      const fb = this.fallbackPath;
      const router = this.$router;
      const prev = router._previousRoute;

      const badPrev = prev && prev.name &&
        ['LoginPhone', 'RegisterPhone'].includes(String(prev.name));

      if (
        prev &&
        prev.fullPath &&
        prev.fullPath !== this.$route.fullPath &&
        !badPrev &&
        prev.path !== '/'
      ) {
        router.back();
        return;
      }

      if (fb && fb !== this.$route.path) {
        router.push(fb);
        return;
      }

      router.back();
    }
  }
};
</script>

<style lang="scss" scoped>
.mobile-back-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  min-height: 48px;
  padding: 4px 8px 4px 4px;
  box-sizing: border-box;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);

  &__btn {
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    padding: 8px 10px;
    font-size: 15px;
    color: #409eff;

    i {
      font-size: 18px;
      margin-right: 2px;
    }

    &:hover,
    &:focus {
      color: #66b1ff;
    }
  }

  &__label {
    font-weight: 500;
  }

  &__title {
    flex: 1;
    min-width: 0;
    text-align: center;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    padding: 0 8px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__flex {
    flex: 0 0 56px;
    width: 56px;
  }
}
</style>
