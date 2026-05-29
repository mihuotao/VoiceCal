package com.voicecal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.voicecal.entity.CalendarEvent;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CalendarEventMapper extends BaseMapper<CalendarEvent> {

    List<CalendarEvent> selectByUserIdAndTimeRange(Long userId, String startTime, String endTime);

}
