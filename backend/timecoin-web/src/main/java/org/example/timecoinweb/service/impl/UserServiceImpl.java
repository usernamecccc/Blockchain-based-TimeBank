package org.example.timecoinweb.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.example.pojo.*;
import org.example.timecoinweb.config.BlockchainProperties;
import org.example.timecoinweb.mapper.ActivityMapper;
import org.example.timecoinweb.mapper.AdmiMapper;
import org.example.timecoinweb.mapper.CompensationMapper;
import org.example.timecoinweb.mapper.OldMapper;
import org.example.timecoinweb.mapper.UserMapper;
import org.example.timecoinweb.mapper.VolMapper;
import org.example.timecoinweb.service.TimeCoinChainService;
import org.example.timecoinweb.service.UserService;
import org.example.timecoinweb.util.ServiceTypeSupport;
import org.example.utils.DistanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    ActivityMapper activityMapper;
    @Autowired
    UserMapper userMapper;
    @Autowired
    OldMapper oldMapper;
    @Autowired
    AdmiMapper admiMapper;
    @Autowired
    VolMapper volMapper;
    @Autowired
    DistanceUtils distanceUtils;
    @Autowired
    BlockchainProperties blockchainProperties;
    @Autowired
    TimeCoinChainService timeCoinChainService;
    @Autowired
    CompensationMapper compensationMapper;

    /**
     * 老人增加活动
     * @param activity
     * @param userId
     */
    @Override
    public void oldAddActivity(Activity activity,Integer userId) {
        //用用户io查老人id
        Integer oldId=oldMapper.selectOldId(userId);

        //补充数据
        activity.setRemain(activity.getQuota());
        activity.setCreateTime(LocalDateTime.now());
        activity.setUpdateTime(LocalDateTime.now());
        activity.setOldId(oldId);
        Integer administratorTableId = admiMapper.selectFirstAdministratorId();
        if (administratorTableId == null) {
            throw new IllegalStateException(
                    "系统中还没有管理员账号，请先在电脑端注册并登录一名管理员后再发布活动。");
        }
        activity.setAdministratorId(administratorTableId);

        normalizeVolunteerRewardForPublish(activity);
        ServiceTypeSupport.normalizeForCreate(activity, OBJECT_MAPPER);

        chargePublishFeeOnChain(userId);

        // 老人自主发布：提交待审核（1）
        activity.setStatus((short) 1);

        //将这个活动存入活动表
        activityMapper.insert(activity);
    }

    /** 移动端发布活动：校验每人答谢时间币上限；null 当作 0。 */
    private void normalizeVolunteerRewardForPublish(Activity activity) {
        Integer r = activity.getVolunteerReward();
        if (r == null) {
            activity.setVolunteerReward(0);
            return;
        }
        if (r < 0) {
            throw new IllegalArgumentException("答谢时间币不能为负数");
        }
        BigInteger max = blockchainProperties.getVolunteerRewardMax();
        if (max != null && max.signum() > 0 && BigInteger.valueOf(r).compareTo(max) > 0) {
            throw new IllegalArgumentException("答谢时间币不能超过单次上限 " + max);
        }
    }

    /**
     * 链上扣除发布活动所需时间币（合约 owner 代签 transfer）；未启用链或配置费用为 0 则跳过。
     */
    private void chargePublishFeeOnChain(Integer userId) {
        if (!blockchainProperties.isEnabled() || !timeCoinChainService.isChainReady()) {
            return;
        }
        BigInteger cost = blockchainProperties.getOldPublishActivityCost();
        if (cost == null || cost.signum() <= 0) {
            return;
        }
        String from = String.valueOf(userId);
        String to = blockchainProperties.getFeeRecipientUserId();
        if (!StringUtils.hasText(to)) {
            to = "platform";
        }
        try {
            BigInteger bal = timeCoinChainService.balanceOf(from);
            if (bal.compareTo(cost) < 0) {
                throw new IllegalStateException(
                        "时间币余额不足：发布活动需要 " + cost + "，当前余额 " + bal);
            }
            timeCoinChainService.transfer(from, to, cost);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("老人发布活动链上扣费失败 userId={}", userId, e);
            throw new IllegalStateException("链上扣除时间币失败：" + e.getMessage());
        }
    }

    /**
     * 老人更新或修改活动数据
     * @param activity
     */
    @Override
    public void update(Activity activity) {

        //将更新时间更改了
        activity.setUpdateTime(LocalDateTime.now());
        ServiceTypeSupport.normalizeForUpdate(activity, OBJECT_MAPPER);

        activityMapper.update(activity);
    }

    /**
     * 老人删除活动
     * @param ids
     */
    @Transactional
    @Override
    public void oldDeleteActivity(List<Integer> ids) {
        activityMapper.deleteById(ids);

        //维护与活动表相关的表
        volMapper.deleteVolActByActIds(ids);
    }

    /**
     * 老人搜寻自己的活动
     * @param page
     * @param pageSize
     * @return
     */
    @Override
    public PageBean oldSelectActivity(Integer page, Integer pageSize,Integer userId) {
        //找到老人的老人id
        Integer oldId=oldMapper.selectOldId(userId);

        //1.设置分页参数
        PageHelper.startPage(page, pageSize);

        //2.执行查询
        List<Activity> activityList = activityMapper.selectPageActivity(oldId,null,null,null,null,null,null,null);
        Page<Activity> p=(Page<Activity>) activityList;

        //3.封装PageBean对象
        PageBean pageBean=new PageBean(p.getTotal(),p.getResult());

        return pageBean;
    }

    /**
     * 用户（志愿者）接受活动，找到志愿者id并维护中间表,让remain-1
     * @param activityId
     * @param userId
     */
    @Transactional
    @Override
    public String volAcceptActivity(Integer activityId, Integer userId) {
        //1.找到该志愿者的志愿者id
        Integer volId=volMapper.selectVolId(userId);
        if (volId == null) {
            return "志愿者信息不存在，请重新登录";
        }

        //2.已报名直接返回，避免重复扣减
        VolActivity existed = volMapper.volSelectByActVolId(activityId, volId);
        if (existed != null) {
            return "您已报名该活动";
        }

        //3.仅在剩余名额>0时扣减
        int affected = activityMapper.lessRemain(activityId);
        if (affected <= 0) {
            return "剩余名额不够";
        }

        //4.维护志愿者、活动中间表
        try {
            volMapper.insertVolAct(volId, activityId);
        } catch (DuplicateKeyException e) {
            // 避免扣减成功但重复报名失败导致 remain 不一致
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return "您已报名该活动";
        } catch (DataAccessException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw e;
        }

        return "1";
    }


    @Override
    public PageBean volSelectMine(Integer id,Integer page, Integer pageSize, String title, LocalDate date, LocalTime begin, LocalTime end, String address,Short status) {
        //找到用户的志愿者id
        Integer volId=volMapper.selectVolId(id);

        //用志愿者ID搜寻出活动Id
        List<Integer> actIds=volMapper.selectActIds(volId);

        //1.设置分页参数
        PageHelper.startPage(page, pageSize);

        //2.执行查询
        List<Activity> activityList = volMapper.volSelectPageActivity(volId,null,title, date, begin, end, status, address,actIds);
        Page<Activity> p=(Page<Activity>) activityList;

        //3.封装PageBean对象
        PageBean pageBean=new PageBean(p.getTotal(),p.getResult());

        return pageBean;
    }

    @Transactional
    @Override
    public void volCancel(Integer userId, Integer activityId) throws DataAccessException{
        //找到志愿者id
        Integer volId=volMapper.selectVolId(userId);

        //删除vol_activity表的对应条例，再将remain加1
        //两条语句一气呵成
        volMapper.volCancel(volId,activityId);
        activityMapper.moreRemain(activityId);

    }

    @Override
    public PageBean volSelectActivity(Integer id, Integer page, Integer pageSize, String title, LocalDate date, LocalTime begin, LocalTime end, String address,Short status,LocalDateTime deadline) {
        //志愿者的志愿者ID
        Integer volId=volMapper.selectVolId(id);
        //找到已经报名的活动id
        List<Integer> actIds=volMapper.selectActIds(volId);

        //1.设置分页参数
        PageHelper.startPage(page, pageSize);

        //2.执行查询
        List<Activity> activities=volMapper.volSelectNotActivity(actIds,title,date,begin,end,status,address,deadline);//未报名活动
        Page<Activity> p=(Page<Activity>) activities;

        //3.封装PageBean对象
        PageBean pageBean=new PageBean(p.getTotal(),p.getResult());

        return pageBean;
    }

    @Override
    public PageBean oldSelectVolStatus(Integer id, Integer page, Integer pageSize) {
        //1.设置分页参数
        PageHelper.startPage(page, pageSize);

        //2.执行查询
        List<Vol> users=volMapper.selectUsers(id);
        Page<Vol> p=(Page<Vol>) users;

        //3.封装PageBean对象
        PageBean pageBean=new PageBean(p.getTotal(),p.getResult());

        return pageBean;
    }

    /**
     * 老人将志愿者标记「完成」且活动设有答谢时：在本次事务内对已报名行加锁后继签链上答谢，再进行状态写入。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateVolActivity(VolActivity volActivity) {
        Integer volId = volMapper.selectVolId(volActivity.getUserId());
        volActivity.setVolId(volId);

        Short newStatus = volActivity.getStatus();

        Activity activitySnap = null;
        VolActivity locked = null;

        boolean shouldPayReward = false;
        
        locked = volMapper.lockVolunteerActivityRow(volActivity.getActivityId(), volId);
        if (locked == null) {
            throw new IllegalStateException("未找到该志愿者的报名信息");
        }

        boolean isMarkingComplete = (newStatus != null && newStatus == 1);
        boolean wasNotCompleted = locked.getStatus() == null || locked.getStatus() != 1;
        boolean rewardNotPaid = locked.getRewardPaid() == null || locked.getRewardPaid() != 1;

        if (isMarkingComplete) {
            activitySnap = activityMapper.selectByActId(volActivity.getActivityId());
            if (activitySnap == null) {
                throw new IllegalStateException("活动不存在");
            }
            
            int rewardPerVolunteer =
                    activitySnap.getVolunteerReward() == null ? 0 : activitySnap.getVolunteerReward();
            shouldPayReward = wasNotCompleted && rewardPerVolunteer > 0 && rewardNotPaid;
        }

        if (isMarkingComplete) {
            if (shouldPayReward) {
                int rewardPerVolunteer =
                        activitySnap.getVolunteerReward() == null ? 0 : activitySnap.getVolunteerReward();
                if (!blockchainProperties.isEnabled() || !timeCoinChainService.isChainReady()) {
                    throw new IllegalStateException(
                            "本活动设置了志愿者答谢时间币，当前区块链不可用，无法标记完成。请稍后重试或先在发布端将答谢改为 0。");
                }
                transferVolunteerRewardOnChain(activitySnap, volId, rewardPerVolunteer);
                volActivity.setRewardPaid((short) 1);
            } else if (locked.getRewardPaid() != null) {
                volActivity.setRewardPaid(locked.getRewardPaid());
            }
        } else {
            // 撤回完成：若已付过答谢，由平台垫付退还给老人
            if (locked.getRewardPaid() != null && locked.getRewardPaid() == 1) {
                activitySnap = activityMapper.selectByActId(volActivity.getActivityId());
                if (activitySnap == null) {
                    throw new IllegalStateException("活动不存在");
                }
                int rewardPerVolunteer =
                        activitySnap.getVolunteerReward() == null ? 0 : activitySnap.getVolunteerReward();
                if (rewardPerVolunteer > 0) {
                    if (!blockchainProperties.isEnabled() || !timeCoinChainService.isChainReady()) {
                        throw new IllegalStateException(
                                "区块链不可用，无法执行平台垫付补偿，请稍后重试。");
                    }
                    Integer elderUserId = oldMapper.selectUserId(activitySnap.getOldId());
                    compensateElderOnChain(activitySnap, volId, elderUserId, rewardPerVolunteer);
                }
            }
            volActivity.setRewardPaid((short) 0);
        }

        volActivity.setUpdateTime(LocalDateTime.now());
        volMapper.update(volActivity);
    }

    /** 每名志愿者仅能成功答谢一次（行锁保障，同一事务内更新 status + reward_paid）。链上不可逆。 */
    private void transferVolunteerRewardOnChain(Activity act, int volunteerTableId, int rewardUnits) throws IllegalStateException {
        Integer elderUserId = oldMapper.selectUserId(act.getOldId());
        Integer volunteerUserId = volMapper.selectUserId(volunteerTableId);
        if (elderUserId == null || volunteerUserId == null) {
            throw new IllegalStateException("无法解析老人或志愿者的用户编号，无法进行链上答谢");
        }
        BigInteger amount = BigInteger.valueOf(rewardUnits);
        String fromId = String.valueOf(elderUserId);
        String toId = String.valueOf(volunteerUserId);
        try {
            BigInteger balance = timeCoinChainService.balanceOf(fromId);
            if (balance.compareTo(amount) < 0) {
                throw new IllegalStateException(
                        "老人链上余额不足本次答谢所需 " + amount + " 时间币，当前余额 " + balance + "（请先为该老人 mint 或减少答谢金额后再标记完成）");
            }
            timeCoinChainService.transfer(fromId, toId, amount);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn(
                    "志愿者答谢划转失败 activityId={} volunteerTableId={}", act.getId(), volunteerTableId, e);
            throw new IllegalStateException("链上答谢失败：" + e.getMessage());
        }
    }

    /** 老人撤回完成时，由平台账户垫付退还答谢时间币，并记录待追讨补偿。 */
    private void compensateElderOnChain(Activity act, int volunteerTableId, Integer elderUserId, int rewardUnits) throws IllegalStateException {
        if (elderUserId == null) {
            throw new IllegalStateException("无法解析老人的用户编号，无法进行平台垫付");
        }
        BigInteger amount = BigInteger.valueOf(rewardUnits);
        String fromId = blockchainProperties.getFeeRecipientUserId();
        if (!StringUtils.hasText(fromId)) {
            fromId = "platform";
        }
        String toId = String.valueOf(elderUserId);
        try {
            BigInteger balance = timeCoinChainService.balanceOf(fromId);
            if (balance.compareTo(amount) < 0) {
                throw new IllegalStateException(
                        "平台账户余额不足本次垫付所需 " + amount + " 时间币，当前余额 " + balance + "，请联系管理员充值平台账户后再撤回。");
            }
            timeCoinChainService.transfer(fromId, toId, amount);
            CompensationRecord record = new CompensationRecord();
            record.setActivityId(act.getId());
            record.setVolunteerTableId(volunteerTableId);
            record.setElderUserId(elderUserId);
            record.setAmount(rewardUnits);
            record.setStatus((short) 0);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            compensationMapper.insert(record);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("平台垫付补偿失败 activityId={} volunteerTableId={} elderUserId={}",
                    act.getId(), volunteerTableId, elderUserId, e);
            throw new IllegalStateException("平台垫付失败：" + e.getMessage());
        }
    }

    @Override
    public void updateImage(String local, Integer id) {
        userMapper.updateImage(local, id);
    }

    /** 兼容 MyBatis Map 键名大小写不一致（如 ROLE / role） */
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

    @Override
    public java.util.Map<String, Object> getUserStats() {
        List<java.util.Map<String, Object>> roleStats = userMapper.countUsersByRole();
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        for (java.util.Map<String, Object> roleStat : roleStats) {
            Object role = mapGetIgnoreCase(roleStat, "user_role", "role");
            Object count = mapGetIgnoreCase(roleStat, "role_cnt", "count");
            if (role != null) {
                String roleKey = "";
                switch (role.toString()) {
                    case "1": roleKey = "elder"; break;
                    case "2": roleKey = "volunteer"; break;
                    case "3": roleKey = "admin"; break;
                }
                if (!roleKey.isEmpty()) {
                    long n = 0;
                    if (count instanceof Number) {
                        n = ((Number) count).longValue();
                    } else if (count != null) {
                        n = Long.parseLong(count.toString());
                    }
                    stats.put(roleKey, n);
                }
            }
        }
        return stats;
    }

    @Transactional
    @Override
    public Boolean volSignIn(Activity activity, Integer userId) {
        //1.得到志愿者id
        Integer volId=volMapper.selectVolId(userId);
        //2.找到该活动的地址
        String actAddress=activityMapper.selectAddress(activity);

        //3.保存志愿者用户的签到列表
        String userAddress=activity.getAddress();

        //4.检查用户地址与活动地址是否匹配
        double maxDistanceInMeters=100;//设置最大距离为100
        boolean judge= distanceUtils.isWithinDistance(userAddress,actAddress,maxDistanceInMeters);

        if(judge){
            volMapper.volSignIn(volId,activity.getId());

            return true;
        }

        return false;
    }

    @Override
    public VolActivity volCheckMine(Integer activityId, Integer userId) {
        Integer volId=volMapper.selectVolId(userId);

        //用志愿者id和活动id找信息
        VolActivity volActivity=volMapper.volSelectByActVolId(activityId,volId);
        if(volActivity!=null) {
            volActivity.setUserId(userId);
        }

        return volActivity;
    }
}
