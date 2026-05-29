package com.voicecal.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReminderVO {

    private Long id;
    private Long eventId;
    private LocalDateTime remindAt;
    private Integer remindMinutesBefore;
    private String method;
    private Boolean isSent;
    private LocalDateTime sentAt;
    private String status;
    private LocalDateTime createdAt;

}
