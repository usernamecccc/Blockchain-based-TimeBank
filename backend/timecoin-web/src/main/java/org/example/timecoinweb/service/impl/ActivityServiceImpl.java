package org.example.timecoinweb.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.example.pojo.Activity;
import org.example.pojo.PageBean;
import org.example.timecoinweb.mapper.ActivityMapper;
import org.example.timecoinweb.mapper.AdmiMapper;
import org.example.timecoinweb.mapper.OldMapper;
import org.example.timecoinweb.mapper.VolMapper;
import org.example.timecoinweb.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class ActivityServiceImpl implements ActivityService {

    private static Object mapGetIgnoreCase(Map<String, Object> row, String... keyCandidates) {
        for (String want : keyCandidates) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(want)) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    @Autowired
    ActivityMapper activityMapper;
    @Autowired
    VolMapper volMapper;
    @Autowired
    AdmiMapper admiMapper;
    @Autowired
    OldMapper oldMapper;

    /**
     * 前端「老人ID」通常填 user.id；库中外键 activity.old_id 引用的是 old.id。
     * 若传入值已是 old 表主键则保持不变；否则按 user_id 查 old.id。
     */
    private void resolveOldIdForActivity(Activity activity) {
        int raw = activity.getOldId();
        if (oldMapper.selectUserId(raw) != null) {
            return;
        }
        Integer oldPk = oldMapper.selectOldId(raw);
        if (oldPk != null) {
            activity.setOldId(oldPk);
            return;
        }
        throw new IllegalStateException(
                "老人ID无效：请填写已在系统中注册的老人「用户ID」（用户管理中对应用户的 id），"
                        + "或老人档案表主键 old.id。若尚无老人用户，请先在用户管理中添加角色为老人的账号。");
    }


    @Override
    public PageBean selectPageActivity(Integer page, Integer pageSize, String title, LocalDate date, LocalTime begin, LocalTime end, Short status, String address) {
        //1.设置分页参数
        PageHelper.startPage(page, pageSize);

        //2.执行查询
        List<Activity> activityList = activityMapper.selectPageActivity(null,title, date, begin, end, status, address,null);
        Page<Activity> p=(Page<Activity>) activityList;

        //3.封装PageBean对象
        PageBean pageBean=new PageBean(p.getTotal(),p.getResult());

        return pageBean;
    }

    @Transactional
    @Override
    public void deleteById(List<Integer> ids) {
        //删除活动表的的对应项
        activityMapper.deleteById(ids);
        //维护与活动表相关的表
        volMapper.deleteVolActByActIds(ids);
    }

    @Override
    public void update(Activity activity,Integer userId) {
        //将userId转换为administratorId
        Integer administrator=admiMapper.selectAdministratorId(userId);
        //放入到activity
        activity.setAdministratorId(administrator);

        activity.setUpdateTime(LocalDateTime.now());

        resolveOldIdForActivity(activity);
        activityMapper.update(activity);
    }

    @Override
    public void add(Activity activity,Integer userId) {
        //将userId转换为administratorId
        Integer administrator=admiMapper.selectAdministratorId(userId);
        //放入到activity
        activity.setAdministratorId(administrator);

        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        activity.setRemain(activity.getQuota());

        resolveOldIdForActivity(activity);
        activityMapper.insert(activity);
    }

    @Override
    public void refreshActivityLifecycle() {
        activityMapper.markActivitiesExpired();
        activityMapper.promoteApprovedToOngoing();
    }

    @Override
    public Activity selectById(Integer id) {
        return activityMapper.selectByActId(id);
    }

    @Override
    public java.util.Map<String, Object> getActivityStats() {
        List<java.util.Map<String, Object>> statusStats = activityMapper.countActivitiesByStatus();
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        java.util.Map<String, Object> counts = new java.util.HashMap<>();
        for (java.util.Map<String, Object> statusStat : statusStats) {
            Object status = mapGetIgnoreCase(statusStat, "act_status", "status");
            Object count = mapGetIgnoreCase(statusStat, "status_cnt", "count");
            if (status != null) {
                String statusKey = "";
                switch (status.toString()) {
                    case "1": statusKey = "pending"; break;
                    case "2": statusKey = "approved"; break;
                    case "3": statusKey = "ongoing"; break;
                    case "4": statusKey = "rejected"; break;
                    case "5": statusKey = "expired"; break;
                }
                if (!statusKey.isEmpty()) {
                    long n = 0;
                    if (count instanceof Number) {
                        n = ((Number) count).longValue();
                    } else if (count != null) {
                        n = Long.parseLong(count.toString());
                    }
                    counts.put(statusKey, n);
                }
            }
        }
        stats.put("statusDistribution", counts);
        stats.put("totalEngagement", activityMapper.countTotalEngagement());
        return stats;
    }
}
