package com.wuxl.englishcoach.infrastructure.persistence.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private Integer dailyMinutes;
    private String studyStartTime;
    private String reviewTime;
    private String status;
}
