package com.voicecal.controller;

import com.voicecal.common.ApiResult;
import com.voicecal.model.vo.FestivalVO;
import com.voicecal.service.FestivalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/festivals")
public class FestivalController {

    private final FestivalService festivalService;

    public FestivalController(FestivalService festivalService) {
        this.festivalService = festivalService;
    }

    @GetMapping
    public ApiResult<List<FestivalVO>> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResult.success(festivalService.getByDate(date));
    }

    @GetMapping("/today")
    public ApiResult<List<FestivalVO>> getToday() {
        return ApiResult.success(festivalService.getToday());
    }

    @GetMapping("/month")
    public ApiResult<List<FestivalVO>> getByMonth(
            @RequestParam int year, @RequestParam int month) {
        return ApiResult.success(festivalService.getByMonth(year, month));
    }

    @GetMapping("/year")
    public ApiResult<List<FestivalVO>> getByYear(@RequestParam int year) {
        return ApiResult.success(festivalService.getByYear(year));
    }

}
