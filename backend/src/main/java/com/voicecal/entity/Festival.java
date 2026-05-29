package com.voicecal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("festival")
public class Festival {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private LocalDate date;

    private String type;

    private String description;

    private String greeting;

    private String suggestions;

    private String icon;

    private Boolean isLunar;

    private String lunarDate;

    private LocalDateTime createdAt;

}
