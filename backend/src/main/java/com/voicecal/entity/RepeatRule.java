package com.voicecal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repeat_rule")
public class RepeatRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long eventId;

    private String ruleType;

    private Integer intervalValue;

    private String daysOfWeek;

    private String daysOfMonth;

    private String monthlyType;

    private Integer weekOfMonth;

    private String endType;

    private Integer endCount;

    private LocalDateTime endDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
