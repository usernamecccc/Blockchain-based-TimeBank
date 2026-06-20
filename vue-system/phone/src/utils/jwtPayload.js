/**
 * 仅从 JWT payload 读取角色（不做签名校验；权限以后端为准）。
 * @param {string} token
 * @returns {number|null}
 */
export function parseJwtRole(token) {
  if (!token || typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length < 2) return null;
  try {
    let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const pad = base64.length % 4;
    if (pad) base64 += '='.repeat(4 - pad);
    const payload = JSON.parse(atob(base64));
    const r = payload.role;
    return r !== undefined && r !== null ? Number(r) : null;
  } catch {
    return null;
  }
}
