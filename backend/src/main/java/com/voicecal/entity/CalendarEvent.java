package com.voicecal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("calendar_event")
public class CalendarEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

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

    private String source;

    private Boolean isRecurring;

    private Long parentEventId;

    private LocalDate originalDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
