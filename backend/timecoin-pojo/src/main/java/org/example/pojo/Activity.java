package org.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Activity {
    private Integer id;
    private String title;
    private Short quota;
    private LocalDateTime deadline;
    private LocalDate date;
    private LocalTime begin;
    private LocalTime end;
    private String address;
    private int oldId;
    private String phone;
    private String description;
    private Short status;
    private int administratorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String message;
    /** 服务类型编码：medical_rehab/health_manage/cleaning/shopping_companion/clinic_companion/purchase/other_service */
    private String serviceType;
    /** 与 serviceType 对应的扩展字段 JSON */
    private String extraJson;
    private Short remain;
    /** 每名志愿者完成后老人答谢的时间币（整数）；0 表示不向志愿者链上答谢 */
    private Integer volunteerReward;

    /** 当前志愿者在该活动的签到状态（仅「我的活动」列表查询时填充）：1=已签到 */
    private Short volSign;
    /** 当前志愿者服务完成状态（老人确认）：1=已完成 */
    private Short volActStatus;
    /** 当前志愿者是否已收到答谢时间币：1=已答谢 */
    private Short volRewardPaid;
}
