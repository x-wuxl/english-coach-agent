package com.wuxl.englishcoach.infrastructure.persistence.session;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("attempt_log")
public class AttemptLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String attemptCode;
    private Long userId;
    private Long learningItemId;
    private Long studySessionId;
    private String mode;
    private String result;
    private String responseText;
    private Integer responseTimeMs;
    private Boolean hintUsed;
    private String errorType;
    private String errorDetails;
    private LocalDateTime occurredAt;
    private String llmExplanationSnapshot;
    private LocalDateTime createdAt;
}
