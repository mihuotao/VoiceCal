package com.voicecal.model.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EventQuery {

    private LocalDate startDate;

    private LocalDate endDate;

    private String category;

    private String status;

    private Integer priority;

    private String keyword;

    private String source;

}
