<template>
  <body>
    <div id="app" class="phone-app-root" :class="{ 'phone-app-root--nav': showMobileBackBar }">
      <mobile-back-bar
        v-if="showMobileBackBar"
        :title="mobileBackTitle"
        :fallback-path="mobileBackFallbackResolved"
      />
      <router-view />
    </div>
  </body>
</template>

<script>
import MobileBackBar from '@/components/MobileBackBar.vue'
import resolveMobileBackFallback from '@/utils/mobileBackFallback'

export default {
  name: 'App',
  components: {
    MobileBackBar
  },
  computed: {
    showMobileBackBar() {
      if (this.$route.meta.hideMobileBack) return false
      if (this.$route.name === 'PageNotFound') return false
      return true
    },
    mobileBackTitle() {
      return (this.$route.meta && this.$route.meta.mobileBackTitle) || ''
    },
    mobileBackFallbackResolved() {
      const prev = this.$router._previousRoute
      return resolveMobileBackFallback(this.$route, prev)
    },
  },
}
</script>

<style>
body {
  margin: 0;
  padding: 0;
  border: 0;
  background: #f2f8fb;
  color: #1f2d3d;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.phone-app-root--nav {
  box-sizing: border-box;
  min-height: 100vh;
  padding-top: 48px;
}

:root {
  --old-bg: #f2f8fb;
  --old-surface: #ffffff;
  --old-primary: #b8511f;
  --old-primary-strong: #944018;
  --old-text: #1f2d3d;
  --old-muted: #5d6b78;
  --old-border: #d4e8ef;
  --vol-bg: #f2f8fb;
  --vol-surface: #ffffff;
  --vol-primary: #1677a6;
  --vol-primary-strong: #0f5f85;
  --vol-accent: #2f9e84;
  --vol-border: #d4e8ef;
}
</style>
