package com.wuxl.englishcoach.infrastructure.persistence.review;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("weekly_review_snapshot")
public class WeeklyReviewSnapshotDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private BigDecimal completionRate;
    private Integer studyMinutes;
    private Integer newItemsCount;
    private Integer reviewItemsCount;
    private String highFrequencyErrorTypes;
    private String strongestThemes;
    private String weakestThemes;
    private String nextWeekSuggestion;
    private LocalDateTime createdAt;
}
