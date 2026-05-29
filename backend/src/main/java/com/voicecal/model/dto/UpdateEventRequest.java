package com.voicecal.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateEventRequest {

    private String title;

    private String description;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Boolean allDay;

    private String location;

    private String category;

    private String color;

    private Integer priority;

    private String status;

}
