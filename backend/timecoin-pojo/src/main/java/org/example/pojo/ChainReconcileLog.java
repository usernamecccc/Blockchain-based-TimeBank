package org.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChainReconcileLog {
    private Integer id;
    private LocalDateTime runTime;
    private Integer totalChecked;
    private Integer mismatchCount;
    private Integer fixedCount;
    private Short chainReady;
    private String detailJson;
    private LocalDateTime createTime;
}
