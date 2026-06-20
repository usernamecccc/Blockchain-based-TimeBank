import { formatActivityDates, formatVolunteerRewardAmount, parseDateTime } from './volunteerActivity';

export { formatActivityDates, formatVolunteerRewardAmount };

const AUDIT_STATUS_MAP = {
  1: { label: '待审核', type: 'warning' },
  2: { label: '审核通过', type: 'success' },
  3: { label: '进行中', type: 'primary' },
  4: { label: '已拒绝', type: 'danger' },
  5: { label: '已过期', type: 'info' },
};

const SERVICE_TYPE_MAP = {
  medical_rehab: '医疗康复',
  health_manage: '健康管理',
  cleaning: '清洁整理',
  shopping_companion: '购物陪同',
  clinic_companion: '问诊陪护',
  purchase: '物品代购',
  other_service: '其他服务',
};

export function formatServiceTypeLabel(serviceType) {
  if (!serviceType) return SERVICE_TYPE_MAP.other_service;
  return SERVICE_TYPE_MAP[serviceType] || SERVICE_TYPE_MAP.other_service;
}

export function getAuditStatusTag(status) {
  return AUDIT_STATUS_MAP[status] || { label: '未知', type: 'info' };
}

export function getRegistrationTag(row) {
  const deadline = parseDateTime(row && row.deadline);
  if (!deadline) return { label: '报名中', type: 'success' };
  return new Date() < deadline
    ? { label: '报名中', type: 'success' }
    : { label: '报名截止', type: 'danger' };
}

export function quotaPercent(row) {
  const quota = Number(row && row.quota);
  const remain = Number(row && row.remain);
  if (!Number.isFinite(quota) || quota <= 0) return 0;
  const filled = quota - (Number.isFinite(remain) ? remain : 0);
  return Number(((filled / quota) * 100).toFixed(1));
}
