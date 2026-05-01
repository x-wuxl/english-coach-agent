package com.wuxl.englishcoach.infrastructure.persistence.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user_profile")
public class UserProfileDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userCode;
    private String goal;
    private String subGoals;
    private Integer dailyMinutes;
    private String studyStartTime;
    private String reviewTime;
    private String overallLevel;
    private String vocabLevel;
    private String grammarLevel;
    private String readingLevel;
    private String outputLevel;
    private String preferredModes;
    private String motivationStyle;
    private String fatigueTolerance;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
