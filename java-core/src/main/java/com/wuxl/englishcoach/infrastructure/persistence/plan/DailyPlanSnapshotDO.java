package com.wuxl.englishcoach.infrastructure.persistence.plan;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("daily_plan_snapshot")
public class DailyPlanSnapshotDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planCode;
    private Long userId;
    private LocalDate planDate;
    private String planType;
    private String status;
    private Integer totalNewCount;
    private Integer totalReviewCount;
    private Integer totalOutputCount;
    private String loadReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
