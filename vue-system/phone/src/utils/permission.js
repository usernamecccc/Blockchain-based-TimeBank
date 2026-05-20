// 登录页路由为 '/'
const LOGIN_PAGE_URL = '/';
import store from '@/store';
import { parseJwtRole } from '@/utils/jwtPayload';

/** 志愿者端菜单 / 接口路径（对应后端 URL 须包含 vol）。说明：`/infoPhone` 为个人信息（走 /info），老人端右上角设置也会进入，不作为志愿者专区拦截。 */
const VOLUNTEER_PATHS = new Set([
  '/dashPhone',
  '/homePhone',
  '/addActivityPhone',
  '/infoOfUserPhone',
  '/artificialVolunteer',
  '/targetPage',
  '/activityOfUser',
  '/registeredActivity',
  '/signInUser',
]);

/** 老人端路径（对应后端 URL 须包含 old） */
const OLD_PATHS = new Set([
  '/dashOld',
  '/homeOld',
  '/serverOld',
  '/artificialOld',
  '/activityOld',
  '/addActivityOld',
  '/locationGet',
  '/getInfoActivity',
  '/endAddActivity',
  '/idActivityOld',
]);

function checkToken() {
  return !!store.getters.getToken;
}

function pathZone(path) {
  if (VOLUNTEER_PATHS.has(path)) return 'vol';
  if (OLD_PATHS.has(path)) return 'old';
  return null;
}

export function requireAuth(to, from, next) {
  if (!checkToken()) {
    next({ path: LOGIN_PAGE_URL });
    return;
  }

  const role = parseJwtRole(store.getters.getToken);
  const zone = pathZone(to.path);

  if (zone === 'vol' && role !== 2) {
    if (role === 1) next('/serverOld');
    else next('/');
    return;
  }

  if (zone === 'old' && role !== 1) {
    if (role === 2) next('/homePhone');
    else next('/');
    return;
  }

  next();
}

/**
 * 根路径：曾有无条件跳转 /homePhone，导致老人 token 误入志愿者首页并触发 ACCESS_RESTRICTED。
 */
export function loginrequireAuth(to, from, next) {
  if (checkToken() && to.path === '/') {
    const role = parseJwtRole(store.getters.getToken);
    if (role === 2) next('/homePhone');
    else next('/serverOld');
    return;
  }
  next();
}
