package org.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChainAccountMirror {
    private String accountId;
    private Long coinBalance;
    private LocalDateTime balanceSyncTime;
    private LocalDateTime updateTime;
}
