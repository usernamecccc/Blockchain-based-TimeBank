package org.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChainTxLog {
    private Long id;
    private String txType;
    private String fromAccount;
    private String toAccount;
    private Long amount;
    private String txHash;
    private String bizType;
    private String bizRef;
    private LocalDateTime createTime;
}
