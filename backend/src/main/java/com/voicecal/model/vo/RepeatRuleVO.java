package com.voicecal.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepeatRuleVO {

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
    private String createdAt;

}
