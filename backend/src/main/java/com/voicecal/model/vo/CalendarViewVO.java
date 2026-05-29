package com.voicecal.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class CalendarViewVO {

    private String view;
    private String currentDate;
    private List<CalendarDayVO> days;
    private List<FestivalVO> festivals;

    @Data
    public static class CalendarDayVO {
        private String date;
        private Boolean isCurrentMonth;
        private Boolean isToday;
        private String lunarDate;
        private List<FestivalVO> festivals;
        private List<EventVO> events;
    }

    @Data
    public static class FestivalVO {
        private Long id;
        private String name;
        private String date;
        private String type;
        private String greeting;
        private String icon;
        private Boolean isLunar;
        private String lunarDate;
    }

}
