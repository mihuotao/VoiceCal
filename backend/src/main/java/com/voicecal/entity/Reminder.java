package com.voicecal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reminder")
public class Reminder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long eventId;

    private Long userId;

    private LocalDateTime remindAt;

    private Integer remindMinutesBefore;

    private String method;

    private Boolean isSent;

    private LocalDateTime sentAt;

    private String status;

    private LocalDateTime createdAt;

}
