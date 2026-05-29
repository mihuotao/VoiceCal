package com.voicecal.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateRepeatRuleRequest {

    private String ruleType;

    private Integer intervalValue;

    private String daysOfWeek;

    private String daysOfMonth;

    private String monthlyType;

    private Integer weekOfMonth;

    private String endType;

    private Integer endCount;

    private LocalDateTime endDate;

}
