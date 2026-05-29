package com.voicecal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("festival_greeting_log")
public class FestivalGreetingLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long festivalId;

    private Long eventId;

    private String greetingType;

    private String greetingText;

    private Boolean isTtsSent;

    private String userAction;

    private LocalDateTime createdAt;

}
