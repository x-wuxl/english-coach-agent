package com.wuxl.englishcoach.infrastructure.persistence.coach;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("coach_turn")
public class CoachTurnDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coachSessionId;
    private String mode;
    private String userMessage;
    private String coachMessage;
    private String detectedNotes;
    private LocalDateTime createdAt;
}
