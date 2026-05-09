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
}

.phone-app-root--nav {
  box-sizing: border-box;
  min-height: 100vh;
  padding-top: 48px;
}
</style>
