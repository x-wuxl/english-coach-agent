package com.wuxl.englishcoach.infrastructure.persistence.memory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("expression_gap")
public class ExpressionGapDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String gapKey;
    private String zhIntent;
    private String naturalExpressions;
    private String userAttempts;
    private String context;
    private Integer seenCount;
    private String status;
    private LocalDateTime lastSeenAt;
    private LocalDateTime nextDrillAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
