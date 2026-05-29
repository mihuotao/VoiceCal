package com.voicecal.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventVO {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String startTime;
    private String endTime;
    private Boolean allDay;
    private String location;
    private String category;
    private String color;
    private Integer priority;
    private String status;
    private String source;
    private Boolean isRecurring;
    private String createdAt;
    private String updatedAt;

}
