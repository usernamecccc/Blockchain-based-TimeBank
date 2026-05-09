package org.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VolActivity {
    Integer id;
    Integer userId;
    Integer volId;
    Integer activityId;
    Short status;
    Short sign;
    /** 0 未付答谢 / 1 已付链上答谢 */
    Short rewardPaid;
    LocalDateTime createTime;
    LocalDateTime updateTime;
}
