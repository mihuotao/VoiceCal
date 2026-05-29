package com.voicecal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.voicecal.entity.Reminder;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReminderMapper extends BaseMapper<Reminder> {

    List<Reminder> selectPendingBefore(String before);

}
