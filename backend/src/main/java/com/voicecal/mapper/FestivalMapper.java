package com.voicecal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.voicecal.entity.Festival;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FestivalMapper extends BaseMapper<Festival> {

    List<Festival> selectByMonth(int year, int month);

}
