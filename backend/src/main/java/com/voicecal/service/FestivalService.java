package com.voicecal.service;

import com.voicecal.entity.Festival;
import com.voicecal.model.vo.FestivalVO;

import java.time.LocalDate;
import java.util.List;

public interface FestivalService {

    List<FestivalVO> getByDate(LocalDate date);

    List<FestivalVO> getToday();

    List<FestivalVO> getByMonth(int year, int month);

    List<FestivalVO> getByYear(int year);

}
