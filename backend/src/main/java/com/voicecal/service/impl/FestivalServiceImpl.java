package com.voicecal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.voicecal.entity.Festival;
import com.voicecal.mapper.FestivalMapper;
import com.voicecal.model.vo.FestivalVO;
import com.voicecal.service.FestivalService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FestivalServiceImpl implements FestivalService {

    private final FestivalMapper festivalMapper;

    public FestivalServiceImpl(FestivalMapper festivalMapper) {
        this.festivalMapper = festivalMapper;
    }

    @Override
    public List<FestivalVO> getByDate(LocalDate date) {
        List<Festival> list = festivalMapper.selectList(
                new LambdaQueryWrapper<Festival>()
                        .eq(Festival::getDate, date)
                        .orderByAsc(Festival::getType));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<FestivalVO> getToday() {
        return getByDate(LocalDate.now());
    }

    @Override
    public List<FestivalVO> getByMonth(int year, int month) {
        List<Festival> list = festivalMapper.selectByMonth(year, month);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<FestivalVO> getByYear(int year) {
        List<Festival> list = festivalMapper.selectList(
                new LambdaQueryWrapper<Festival>()
                        .isNotNull(Festival::getDate)
                        .apply("YEAR(date) = {0}", year)
                        .or(w -> w.isNull(Festival::getDate).eq(Festival::getIsLunar, 1))
                        .orderByAsc(Festival::getType, Festival::getDate));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    private FestivalVO toVO(Festival festival) {
        FestivalVO vo = new FestivalVO();
        vo.setId(festival.getId());
        vo.setName(festival.getName());
        vo.setDate(festival.getDate() != null ? festival.getDate().toString() : null);
        vo.setType(festival.getType());
        vo.setDescription(festival.getDescription());
        vo.setGreeting(festival.getGreeting());
        vo.setSuggestions(festival.getSuggestions());
        vo.setIcon(festival.getIcon());
        vo.setIsLunar(festival.getIsLunar());
        vo.setLunarDate(festival.getLunarDate());
        return vo;
    }

}
