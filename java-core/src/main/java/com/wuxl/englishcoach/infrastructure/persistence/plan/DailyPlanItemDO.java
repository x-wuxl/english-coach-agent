package com.wuxl.englishcoach.infrastructure.persistence.plan;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("daily_plan_item")
public class DailyPlanItemDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dailyPlanSnapshotId;
    private Long learningItemId;
    private String itemRole;
    private Integer sequenceNo;
    private String selectionReason;
    private BigDecimal priorityScore;
    private String recommendedMode;
    private LocalDateTime createdAt;
}
