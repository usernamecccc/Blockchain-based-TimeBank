import request from './request';

/** 将后端返回的头像文件名或相对路径转为可访问的完整 URL */
export function resolveAvatarUrl(image) {
  if (image == null || image === '') return '';
  const value = String(image).trim();
  if (!value) return '';
  if (/^https?:\/\//i.test(value) || value.startsWith('data:')) {
    return value;
  }
  const base = (request.defaults.baseURL || '').replace(/\/$/, '');
  if (value.startsWith('/image/')) {
    return `${base}${value}`;
  }
  return `${base}/image/${value.replace(/^\/+/, '')}`;
}
