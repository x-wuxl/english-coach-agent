package com.wuxl.englishcoach.infrastructure.persistence.content;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("learning_item")
public class LearningItemDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String itemCode;
    private String type;
    private String content;
    private String meaningZh;
    private Integer difficulty;
    private String theme;
    private String tags;
    private String examples;
    private String relatedItemCodes;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
