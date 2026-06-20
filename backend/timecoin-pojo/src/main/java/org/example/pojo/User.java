package org.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Integer id;
    private String username;
    private String password;
    private Short role;
    private String email;
    private Short age;
    private String phone;
    private String address;

    private String name;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String image;

    /** 时间币余额镜像（与链上 balanceOf 对账，链为权威数据源） */
    private Long coinBalance;
    /** 最近一次对账/同步时间 */
    private LocalDateTime balanceSyncTime;
}
