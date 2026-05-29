package com.voicecal.model.vo;

import lombok.Data;

@Data
public class FestivalVO {

    private Long id;
    private String name;
    private String date;
    private String type;
    private String description;
    private String greeting;
    private String icon;
    private Boolean isLunar;
    private String lunarDate;

}
