/**
 * 在无历史栈或需要兜底时解析「返回」目标路径。
 * @param {import('vue-router').Route} route
 * @param {import('vue-router').Route} previousRoute
 * @returns {string}
 */
export default function resolveMobileBackFallback(route, previousRoute) {
  const fb = route.meta && route.meta.mobileBackFallback;
  if (typeof fb === 'string' && fb) return fb;

  switch (route.name) {
    case 'CoinInfo':
      if (previousRoute && /Old$/i.test(String(previousRoute.name || ''))) return '/homeOld';
      if (previousRoute && /Old/i.test(String(previousRoute.path || ''))) return '/homeOld';
      return '/infoOfUserPhone';

    case 'InfoPhone':
      if (previousRoute && String(previousRoute.name || '').includes('Old')) return '/homeOld';
      return '/infoOfUserPhone';

    default:
      return route.path.includes('Old') ? '/homeOld' : '/homePhone';
  }
}
