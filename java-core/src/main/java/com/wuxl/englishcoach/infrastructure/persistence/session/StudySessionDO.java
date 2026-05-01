package com.wuxl.englishcoach.infrastructure.persistence.session;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("study_session")
public class StudySessionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionCode;
    private Long userId;
    private LocalDate sessionDate;
    private String sessionType;
    private String status;
    private Integer durationMin;
    private Integer newItemsCount;
    private Integer reviewItemsCount;
    private java.math.BigDecimal accuracy;
    private java.math.BigDecimal completionRate;
    private String fatigueFeedback;
    private String moodFeedback;
    private String focusTheme;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
