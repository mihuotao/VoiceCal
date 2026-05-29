package com.voicecal.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateRepeatRuleRequest {

    @NotBlank(message = "重复类型不能为空")
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
