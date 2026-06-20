import Vue from 'vue'
import VueRouter from 'vue-router'
import RegisterPhone from '@/views/phoneui/volunteer/RegisterPhone.vue'
import HomePhone from '@/views/phoneui/volunteer/HomePhone.vue'
import LoginPhone from '@/views/phoneui/volunteer/LoginPhone.vue'
import InfoOfUserPhone from '@/views/phoneui/volunteer/InfoOfUserPhone.vue'
import AddActivityPhone from '@/views/phoneui/volunteer/AddActivityPhone.vue'
import InfoPhone from '@/views/phoneui/volunteer/InfoPhone.vue'
import TargetPage from '@/views/phoneui/volunteer/TargetPage.vue'
import ActivityOfUser from '@/views/phoneui/volunteer/ActivityOfUser.vue'
import RegisteredActivity from '@/views/phoneui/volunteer/RegisteredActivity.vue'
import SignInUser from '@/views/phoneui/volunteer/SignInUser.vue'
import ArtificialVolunteer from '@/views/phoneui/volunteer/ArtificialVolunteer.vue'
import HomeOld from '@/views/phoneui/old/HomeOld.vue'
import ActivityOld from '@/views/phoneui/old/ActivityOld.vue'
import AddActivityOld from '@/views/phoneui/old/AddActivityOld.vue'
import LocationGet from '@/views/phoneui/old/LocationGet.vue'
import GetInfoActivity from '@/views/phoneui/old/GetInfoActivity.vue'
import EndAddActivity from '@/views/phoneui/old/EndAddActivity.vue'
import ServerOld from '@/views/phoneui/old/ServerOld.vue'
import ArtificialOld from '@/views/phoneui/old/ArtificialOld.vue'
import IdActivityOld from '@/views/phoneui/old/IdActivityOld.vue'
import DashOld from '@/views/phoneui/old/DashOld.vue'
import CoinInfo from '@/views/phoneui/CoinInfo.vue'
import CoinHistory from '@/views/phoneui/CoinHistory.vue'
import DashPhone from '@/views/phoneui/volunteer/DashPhone.vue'
import { requireAuth, loginrequireAuth } from '../utils/permission'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'LoginPhone',
    component: LoginPhone,
    beforeEnter: loginrequireAuth,
    meta: { hideMobileBack: true },
  },
  {
    path: '/registerPhone',
    name: 'RegisterPhone',
    component: RegisterPhone,
    meta: { hideMobileBack: true },
  },
  {
    path: '*',
    name: 'PageNotFound',
    component: () => import('@/views/phoneui/404'),
    hidden: true,
    meta: { hideMobileBack: true },
  },
  {
    path: '/dashPhone',
    name: 'DashPhone',
    component: DashPhone,
    beforeEnter: requireAuth,
    children: [
      {
        path: '/homePhone',
        name: 'HomePhone',
        component: HomePhone,
        beforeEnter: requireAuth,
        meta: { hideMobileBack: true },
      },
      {
        path: '/addActivityPhone',
        name: 'AddActivityPhone',
        component: AddActivityPhone,
        beforeEnter: requireAuth,
        meta: { hideMobileBack: true },
      },
      {
        path: '/infoOfUserPhone',
        name: 'InfoOfUserPhone',
        component: InfoOfUserPhone,
        beforeEnter: requireAuth,
        meta: { hideMobileBack: true },
      },
      {
        path: '/artificialVolunteer',
        name: 'ArtificialVolunteer',
        component: ArtificialVolunteer,
        beforeEnter: requireAuth,
        meta: { hideMobileBack: true },
      },
    ]
  },
  {
    path: '/infoPhone',
    name: 'InfoPhone',
    component: InfoPhone,
    beforeEnter: requireAuth,
    meta: {
      mobileBackTitle: '个人信息',
    },
  },
  {
    path: '/targetPage',
    name: 'TargetPage',
    component: TargetPage,
    beforeEnter: requireAuth,
    meta: {
      mobileBackTitle: '活动详情',
      mobileBackFallback: '/homePhone',
    },
  },
  {
    path: '/activityOfUser',
    name: 'ActivityOfUser',
    component: ActivityOfUser,
    beforeEnter: requireAuth,
    meta: {
      mobileBackTitle: '参与活动',
      mobileBackFallback: '/infoOfUserPhone',
    },
  },
  {
    path: '/registeredActivity',
    name: 'RegisteredActivity',
    component: RegisteredActivity,
    beforeEnter: requireAuth,
    meta: {
      mobileBackTitle: '我已报名',
      mobileBackFallback: '/activityOfUser',
    },
  },
  {
    path: '/signInUser',
    name: 'SignInUser',
    component: SignInUser,
    beforeEnter: requireAuth,
    meta: {
      mobileBackTitle: '活动签到',
      mobileBackFallback: '/registeredActivity',
    },
  },
  {
    path: '/dashOld',
    name: 'DashOld',
    component: DashOld,
    beforeEnter: requireAuth,
    children: [
      {
        path: '/homeOld',
        name: 'HomeOld',
        component: HomeOld,
        beforeEnter: requireAuth,
        meta: { hideMobileBack: true },
      },
      {
        path: '/serverOld',
        name: 'ServerOld',
        component: ServerOld,
        beforeEnter: requireAuth,
        meta: { hideMobileBack: true },
      },
      {
        path: '/artificialOld',
        name: 'ArtificialOld',
        component: ArtificialOld,
        beforeEnter: requireAuth,
        meta: { hideMobileBack: true },
      },
      {
        path: '/activityOld',
        name: 'ActivityOld',
        component: ActivityOld,
        beforeEnter: requireAuth,
        meta: {
          mobileBackTitle: '我的活动',
          mobileBackFallback: '/homeOld',
        },
      },
    ]
  },
  {
    path: '/addActivityOld',
    name: 'AddActivityOld',
    component: AddActivityOld,
    beforeEnter: requireAuth,
    meta: {
      mobileBackTitle: '发起活动',
      mobileBackFallback: '/activityOld',
    },
    children: [
      {
        path: '/locationGet',
        name: 'LocationGet',
        component: LocationGet,
        meta: {
          mobileBackTitle: '选择地点',
          mobileBackFallback: '/getInfoActivity',
        },
      },
      {
        path: '/getInfoActivity',
        name: 'GetInfoActivity',
        component: GetInfoActivity,
        meta: {
          mobileBackTitle: '活动信息',
          mobileBackFallback: '/serverOld',
        },
      },
      {
        path: '/endAddActivity',
        name: 'EndAddActivity',
        component: EndAddActivity,
        meta: {
          mobileBackTitle: '确认提交',
          mobileBackFallback: '/locationGet',
        },
      },
    ]
  },
  {
    path: '/idActivityOld',
    name: 'IdActivityOld',
    component: IdActivityOld,
    beforeEnter: requireAuth,
    meta: {
      mobileBackTitle: '活动详情',
      mobileBackFallback: '/activityOld',
    },
  },
  {
    path: '/coinInfo',
    name: 'CoinInfo',
    component: CoinInfo,
    beforeEnter: requireAuth,
    meta: {
      mobileBackTitle: '时间币详情',
    },
  },
  {
    path: '/coinHistory',
    name: 'CoinHistory',
    component: CoinHistory,
    beforeEnter: requireAuth,
    meta: {
      mobileBackTitle: '转账历史',
    },
  },
]

const router = new VueRouter({
  routes,
})

router.afterEach((to, from) => {
  router._previousRoute = from
})

export default router
