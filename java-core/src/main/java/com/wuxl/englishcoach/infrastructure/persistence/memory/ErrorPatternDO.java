package com.wuxl.englishcoach.infrastructure.persistence.memory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("error_pattern")
public class ErrorPatternDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String patternKey;
    private String label;
    private String descriptionZh;
    private String userExamples;
    private String betterExamples;
    private Integer seenCount;
    private String severity;
    private String status;
    private LocalDateTime lastSeenAt;
    private LocalDateTime nextDrillAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
