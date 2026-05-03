package com.wuxl.englishcoach.infrastructure.persistence.coach;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("coach_session")
public class CoachSessionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionCode;
    private Long userId;
    private String sessionType;
    private String status;
    private String summary;
    private String detectedLevelRange;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
