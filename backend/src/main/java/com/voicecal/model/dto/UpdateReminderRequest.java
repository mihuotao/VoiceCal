package com.voicecal.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateReminderRequest {

    private LocalDateTime remindAt;

    private Integer remindMinutesBefore;

    private String method;

}
