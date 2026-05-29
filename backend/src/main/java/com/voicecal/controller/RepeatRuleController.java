package com.voicecal.controller;

import com.voicecal.auth.CurrentUser;
import com.voicecal.auth.LoginUser;
import com.voicecal.common.ApiResult;
import com.voicecal.common.ResultCode;
import com.voicecal.entity.RepeatRule;
import com.voicecal.model.dto.CreateRepeatRuleRequest;
import com.voicecal.model.dto.UpdateRepeatRuleRequest;
import com.voicecal.model.vo.RepeatRuleVO;
import com.voicecal.service.RepeatRuleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/events/{eventId}/repeat-rule")
public class RepeatRuleController {

    private final RepeatRuleService repeatRuleService;

    public RepeatRuleController(RepeatRuleService repeatRuleService) {
        this.repeatRuleService = repeatRuleService;
    }

    @PostMapping
    public ApiResult<RepeatRuleVO> create(@PathVariable Long eventId,
                                           @CurrentUser LoginUser loginUser,
                                           @Valid @RequestBody CreateRepeatRuleRequest req) {
        RepeatRule rule = repeatRuleService.create(eventId, loginUser.getUserId(), req);
        return ApiResult.created(toVO(rule));
    }

    @GetMapping
    public ApiResult<RepeatRuleVO> get(@PathVariable Long eventId) {
        RepeatRule rule = repeatRuleService.getByEventId(eventId);
        if (rule == null) {
            return ApiResult.notFound("重复规则不存在");
        }
        return ApiResult.success(toVO(rule));
    }

    @PatchMapping
    public ApiResult<RepeatRuleVO> update(@PathVariable Long eventId,
                                           @CurrentUser LoginUser loginUser,
                                           @Valid @RequestBody UpdateRepeatRuleRequest req) {
        try {
            RepeatRule rule = repeatRuleService.update(eventId, loginUser.getUserId(), req);
            return ApiResult.success(toVO(rule));
        } catch (IllegalArgumentException e) {
            return ApiResult.error(ResultCode.NOT_FOUND.getCode(), e.getMessage());
        }
    }

    @DeleteMapping
    public ApiResult<Void> delete(@PathVariable Long eventId,
                                   @CurrentUser LoginUser loginUser) {
        repeatRuleService.delete(eventId, loginUser.getUserId());
        return ApiResult.noContent();
    }

    @PostMapping("/generate")
    public ApiResult<Integer> generate(@PathVariable Long eventId,
                                        @CurrentUser LoginUser loginUser,
                                        @RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate) {
        try {
            int count = repeatRuleService.generateInstances(eventId, loginUser.getUserId(), startDate, endDate);
            return ApiResult.success(count);
        } catch (IllegalArgumentException e) {
            return ApiResult.error(ResultCode.NOT_FOUND.getCode(), e.getMessage());
        }
    }

    private RepeatRuleVO toVO(RepeatRule rule) {
        RepeatRuleVO vo = new RepeatRuleVO();
        vo.setId(rule.getId());
        vo.setEventId(rule.getEventId());
        vo.setRuleType(rule.getRuleType());
        vo.setIntervalValue(rule.getIntervalValue());
        vo.setDaysOfWeek(rule.getDaysOfWeek());
        vo.setDaysOfMonth(rule.getDaysOfMonth());
        vo.setMonthlyType(rule.getMonthlyType());
        vo.setWeekOfMonth(rule.getWeekOfMonth());
        vo.setEndType(rule.getEndType());
        vo.setEndCount(rule.getEndCount());
        vo.setEndDate(rule.getEndDate());
        vo.setCreatedAt(rule.getCreatedAt() != null
                ? rule.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        return vo;
    }

}
