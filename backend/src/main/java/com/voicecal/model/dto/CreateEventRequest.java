package com.voicecal.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateEventRequest {

    @NotBlank(message = "事件标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    private Boolean allDay;

    private String location;

    private String category;

    private String color;

    private Integer priority;

    private RepeatRuleRequest repeatRule;

    private List<ReminderRequest> reminders;

    @Data
    public static class RepeatRuleRequest {
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

    @Data
    public static class ReminderRequest {
        private Integer remindMinutesBefore;
        private String method;
    }

}
