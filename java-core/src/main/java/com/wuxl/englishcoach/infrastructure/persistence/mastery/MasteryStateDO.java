package com.wuxl.englishcoach.infrastructure.persistence.mastery;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("mastery_state")
public class MasteryStateDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long learningItemId;
    private Integer seenCount;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer correctStreak;
    private BigDecimal recognitionScore;
    private BigDecimal recallScore;
    private BigDecimal outputScore;
    private BigDecimal memoryStrength;
    private BigDecimal forgetRisk;
    private LocalDateTime lastSeenAt;
    private LocalDateTime nextReviewAt;
    private String lastMode;
    private String recommendedMode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
