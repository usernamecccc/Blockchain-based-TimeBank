package org.example.timecoinweb.mapper;

import org.apache.ibatis.annotations.*;
import org.example.pojo.Activity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface ActivityMapper {

    /**
     * 查询活动
     * @return
     */
    List<Activity> selectPageActivity(@Param("oldId") Integer oldId,@Param("title") String title,@Param("date") LocalDate date,
                                      @Param("begin") LocalTime begin,@Param("end") LocalTime end,@Param("status") Short status,
                                      @Param("address") String address,@Param("ids") List<Integer> ids);

    /**
     * 批量删除活动
     * @param ids
     */
    void deleteById(@Param("ids") List<Integer> ids);

    /**
     * 更新活动数据
     * @param activity
     */
    void update(Activity activity);

    /**
     * 让活动剩余名额值减少1
     */
    @Update("update activity set remain=remain-1 where id=#{activityId}")
    void lessRemain(@Param("activityId") Integer activityId);

    /**
     * 管理员新增加部门
     * @param activity
     */
    @Insert("insert into activity(title, quota, deadline, date, begin, end, address, old_id, phone, description, administrator_id, create_time, update_time, message, remain) " +
            "values (#{title},#{quota},#{deadline},#{date},#{begin},#{end},#{address},#{oldId},#{phone},#{description},#{administratorId},#{createTime},#{updateTime},#{message},#{remain})")
    void insert(Activity activity);

    /**
     * 用id查询活动
     * @param activityId
     * @return
     */
    @Select("select * from activity where id=#{activityId}")
    Activity selectByActId(@Param("activityId") Integer activityId);

    /**
     * 活动已结束：审核通过(2)或进行中(3) → 已过期(5)
     */
    @Update("UPDATE activity SET status = 5 WHERE status IN (2, 3) AND CONCAT(`date`, ' ', `end`) < NOW()")
    void markActivitiesExpired();

    /**
     * 已到开始时间且尚未结束：审核通过(2) → 进行中(3)
     */
    @Update("UPDATE activity SET status = 3 WHERE status = 2 "
            + "AND CONCAT(`date`, ' ', `begin`) <= NOW() AND CONCAT(`date`, ' ', `end`) >= NOW()")
    void promoteApprovedToOngoing();

    /**
     * remain剩余报名名额+1
     * @param activityId
     */
    @Update("update activity set remain=remain+1 where id=#{activityId}")
    void moreRemain(@Param("activityId") Integer activityId);

    /**
     * 根据活动id查询活动地址
     * @param activity
     * @return
     */
    @Select("select address from activity where id=#{id}")
    String selectAddress(Activity activity);

    /**
     * 统计各状态活动数量
     * @return 包含 status 和 count 的 map 列表
     */
    @Select("SELECT status AS act_status, COUNT(*) AS status_cnt FROM activity GROUP BY status")
    List<java.util.Map<String, Object>> countActivitiesByStatus();

    /**
     * 统计总参与人次 (quota - remain)
     * @return 总参与人次
     */
    @Select("select coalesce(sum(quota - remain), 0) from activity")
    Integer countTotalEngagement();
}
