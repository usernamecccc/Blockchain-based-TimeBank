/** 志愿者端活动列表：时间解析、筛选与状态标签 */

export function parseDateTime(value) {
  if (!value) return null;
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value;
  const normalized = String(value).replace(' ', 'T');
  const parsed = new Date(normalized);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

export function parseActivityTime(date, time) {
  if (!date || !time) return null;
  const normalized = `${date}T${String(time).split('.')[0]}`;
  return parseDateTime(normalized);
}

export function formatActivityDates(activity) {
  const message = activity && activity.message ? String(activity.message) : '';
  if (message) {
    try {
      const parsed = JSON.parse(message);
      if (parsed && Array.isArray(parsed.dates) && parsed.dates.length > 0) {
        return parsed.dates
          .map((item) => String(item).split('-').pop())
          .map((day) => `${parseInt(day, 10)}号`)
          .join(',');
      }
    } catch (error) {
      // ignore malformed legacy message
    }
  }
  if (!activity || !activity.date) return '日期待定';
  const day = String(activity.date).split('-').pop();
  return `${parseInt(day, 10)}号`;
}

export function formatVolunteerRewardAmount(row) {
  const v = row && row.volunteerReward;
  const n = v === null || v === undefined || v === '' ? 0 : Number(v);
  return Number.isFinite(n) ? n : 0;
}

export function quotaPercent(row) {
  const quota = Number(row && row.quota);
  const remain = Number(row && row.remain);
  if (!Number.isFinite(quota) || quota <= 0) return 0;
  const filled = quota - (Number.isFinite(remain) ? remain : 0);
  return Number(((filled / quota) * 100).toFixed(1));
}

export function filledQuota(row) {
  const quota = Number(row && row.quota);
  const remain = Number(row && row.remain);
  if (!Number.isFinite(quota)) return 0;
  return Math.max(0, quota - (Number.isFinite(remain) ? remain : 0));
}

export function isBeforeDeadline(deadline) {
  const deadlineDate = parseDateTime(deadline);
  if (!deadlineDate) return false;
  return new Date() < deadlineDate;
}

const AUDIT_STATUS_MAP = {
  1: { label: '待审核', type: 'warning' },
  2: { label: '审核通过', type: 'success' },
  3: { label: '进行中', type: 'primary' },
  4: { label: '已拒绝', type: 'danger' },
  5: { label: '已过期', type: 'info' },
};

export function getAuditStatusTag(status) {
  return AUDIT_STATUS_MAP[status] || { label: '未知状态', type: 'info' };
}

export function isVolSignedIn(row) {
  return Number(row && row.volSign) === 1;
}

export function isVolServiceCompleted(row) {
  if (!row) return false;
  return Number(row.volActStatus) === 1 || Number(row.volRewardPaid) === 1;
}

/**
 * 活动卡片状态标签（随列表场景变化）
 * @param {'browse'|'joined'|'ended'} scene
 */
export function getActivityCardStatus(row, scene = 'browse') {
  const now = new Date();
  const deadline = parseDateTime(row.deadline);
  const activityStart = parseActivityTime(row.date, row.begin);
  const activityEnd = parseActivityTime(row.date, row.end);
  const remain = Number(row.remain);
  const hasQuota = Number.isFinite(remain) ? remain > 0 : true;
  const signedIn = isVolSignedIn(row);
  const completed = isVolServiceCompleted(row);

  if (row.status === 4) return { label: '已拒绝', type: 'danger' };
  if (row.status === 5) return { label: '已过期', type: 'info' };
  if (row.status === 1) return { label: '待审核', type: 'warning' };

  if (scene === 'joined' || scene === 'ended') {
    if (completed) return { label: '已完成', type: 'success' };
    if (scene === 'ended' || (activityEnd && now > activityEnd)) {
      return signedIn
        ? { label: '已签到', type: 'success' }
        : { label: '已结束', type: 'info' };
    }
    if (activityStart && now < activityStart) {
      return { label: '待开始', type: 'warning' };
    }
    if (activityStart && activityEnd && now >= activityStart && now <= activityEnd) {
      return signedIn
        ? { label: '已签到', type: 'success' }
        : { label: '待签到', type: 'primary' };
    }
    return signedIn
      ? { label: '已签到', type: 'success' }
      : { label: '已报名', type: 'success' };
  }

  if (scene === 'ended' || (activityEnd && now > activityEnd)) {
    return { label: '已结束', type: 'info' };
  }

  if (!hasQuota) return { label: '名额已满', type: 'warning' };
  if (deadline && now >= deadline) return { label: '报名截止', type: 'danger' };

  if (activityStart && activityEnd && now >= activityStart && now <= activityEnd) {
    return { label: '进行中', type: 'primary' };
  }

  if (deadline) {
    const hoursLeft = (deadline - now) / (1000 * 60 * 60);
    if (hoursLeft > 0 && hoursLeft <= 48) {
      return { label: '即将截止', type: 'warning' };
    }
  }

  return { label: '可报名', type: 'success' };
}

export function filterActivitiesByTab(rows, tab, scene = 'browse') {
  const now = new Date();
  return (rows || []).filter((row) => {
    const deadline = parseDateTime(row.deadline);
    const activityEnd = parseActivityTime(row.date, row.end);
    const activityStart = parseActivityTime(row.date, row.begin);
    const remain = Number(row.remain);
    const hasQuota = Number.isFinite(remain) ? remain > 0 : true;

    if (scene === 'browse') {
      if (tab === 'all') return true;
      if (tab === 'available') {
        if (row.status === 4 || row.status === 5 || row.status === 1) return false;
        if (!deadline) return hasQuota;
        return deadline > now && hasQuota;
      }
      if (tab === 'closing') {
        if (!deadline || !hasQuota) return false;
        const hoursLeft = (deadline - now) / (1000 * 60 * 60);
        return hoursLeft > 0 && hoursLeft <= 48;
      }
      if (tab === 'joined' || tab === 'ended') {
        if (tab === 'ended') {
          if (!activityEnd) return !!deadline && deadline <= now;
          return now > activityEnd;
        }
        if (!activityEnd) return true;
        return now <= activityEnd;
      }
      return true;
    }

    if (scene === 'joined') {
      if (tab === 'ended') {
        if (!activityEnd) return !!deadline && deadline <= now;
        return now > activityEnd;
      }
      if (tab === 'ongoing') {
        if (!activityStart || !activityEnd) return false;
        return now >= activityStart && now <= activityEnd;
      }
      // joined / default
      if (!activityEnd) return true;
      return now <= activityEnd;
    }

    if (tab === 'available') {
      if (!deadline) return hasQuota;
      return deadline > now && hasQuota;
    }
    if (tab === 'joined') {
      if (!activityEnd) return true;
      return now <= activityEnd;
    }
    if (tab === 'ended') {
      if (!activityEnd) return !!deadline && deadline <= now;
      return now > activityEnd;
    }
    return true;
  });
}
