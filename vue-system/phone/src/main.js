import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';

Vue.config.productionTip = false

// 将图片路径定义为全局变量
Vue.prototype.$activityImagePath = require('@/assets/common/activity.jpg');
Vue.use(ElementUI, {
  // 全局弹层起始层级；顶栏 MobileBackBar 为 1200，此处保证 Message/Dialog/Loading 等始终在导航之上
  zIndex: 3500,
});

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
