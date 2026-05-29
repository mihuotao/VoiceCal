package com.voicecal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.voicecal.entity.CalendarEvent;
import com.voicecal.entity.RepeatRule;
import com.voicecal.mapper.CalendarEventMapper;
import com.voicecal.mapper.RepeatRuleMapper;
import com.voicecal.model.dto.CreateRepeatRuleRequest;
import com.voicecal.model.dto.UpdateRepeatRuleRequest;
import com.voicecal.service.RepeatRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class RepeatRuleServiceImpl implements RepeatRuleService {

    private final RepeatRuleMapper repeatRuleMapper;
    private final CalendarEventMapper eventMapper;

    public RepeatRuleServiceImpl(RepeatRuleMapper repeatRuleMapper,
                                  CalendarEventMapper eventMapper) {
        this.repeatRuleMapper = repeatRuleMapper;
        this.eventMapper = eventMapper;
    }

    @Override
    public RepeatRule getByEventId(Long eventId) {
        return repeatRuleMapper.selectOne(
                new LambdaQueryWrapper<RepeatRule>()
                        .eq(RepeatRule::getEventId, eventId));
    }

    @Override
    @Transactional
    public RepeatRule create(Long eventId, Long userId, CreateRepeatRuleRequest req) {
        RepeatRule rule = new RepeatRule();
        rule.setEventId(eventId);
        rule.setRuleType(req.getRuleType());
        rule.setIntervalValue(req.getIntervalValue() != null ? req.getIntervalValue() : 1);
        rule.setDaysOfWeek(req.getDaysOfWeek());
        rule.setDaysOfMonth(req.getDaysOfMonth());
        rule.setMonthlyType(req.getMonthlyType());
        rule.setWeekOfMonth(req.getWeekOfMonth());
        rule.setEndType(req.getEndType() != null ? req.getEndType() : "never");
        rule.setEndCount(req.getEndCount());
        rule.setEndDate(req.getEndDate());
        repeatRuleMapper.insert(rule);

        CalendarEvent event = eventMapper.selectById(eventId);
        if (event != null && event.getUserId().equals(userId) && Boolean.FALSE.equals(event.getIsRecurring())) {
            event.setIsRecurring(true);
            eventMapper.updateById(event);
        }

        return rule;
    }

    @Override
    @Transactional
    public RepeatRule update(Long eventId, Long userId, UpdateRepeatRuleRequest req) {
        RepeatRule rule = repeatRuleMapper.selectOne(
                new LambdaQueryWrapper<RepeatRule>().eq(RepeatRule::getEventId, eventId));
        if (rule == null) {
            throw new IllegalArgumentException("重复规则不存在");
        }

        if (req.getRuleType() != null) rule.setRuleType(req.getRuleType());
        if (req.getIntervalValue() != null) rule.setIntervalValue(req.getIntervalValue());
        if (req.getDaysOfWeek() != null) rule.setDaysOfWeek(req.getDaysOfWeek());
        if (req.getDaysOfMonth() != null) rule.setDaysOfMonth(req.getDaysOfMonth());
        if (req.getMonthlyType() != null) rule.setMonthlyType(req.getMonthlyType());
        if (req.getWeekOfMonth() != null) rule.setWeekOfMonth(req.getWeekOfMonth());
        if (req.getEndType() != null) rule.setEndType(req.getEndType());
        if (req.getEndCount() != null) rule.setEndCount(req.getEndCount());
        if (req.getEndDate() != null) rule.setEndDate(req.getEndDate());

        repeatRuleMapper.updateById(rule);
        return rule;
    }

    @Override
    @Transactional
    public void delete(Long eventId, Long userId) {
        repeatRuleMapper.delete(
                new LambdaQueryWrapper<RepeatRule>().eq(RepeatRule::getEventId, eventId));
        CalendarEvent event = eventMapper.selectById(eventId);
        if (event != null && event.getUserId().equals(userId)) {
            event.setIsRecurring(false);
            eventMapper.updateById(event);
        }
    }

    @Override
    @Transactional
    public int generateInstances(Long eventId, Long userId, String startDate, String endDate) {
        RepeatRule rule = getByEventId(eventId);
        if (rule == null) {
            throw new IllegalArgumentException("重复规则不存在");
        }

        CalendarEvent parent = eventMapper.selectById(eventId);
        if (parent == null || !parent.getUserId().equals(userId)) {
            throw new IllegalArgumentException("事件不存在或无权操作");
        }

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now();
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : start.plusMonths(3);
        LocalDate eventStart = parent.getStartTime().toLocalDate();

        List<LocalDate> dates = calculateDates(rule, eventStart, start, end);
        int count = 0;

        for (LocalDate date : dates) {
            if (date.isBefore(start)) continue;
            if (date.isAfter(end)) break;

            long existing = eventMapper.selectCount(
                    new LambdaQueryWrapper<CalendarEvent>()
                            .eq(CalendarEvent::getParentEventId, eventId)
                            .eq(CalendarEvent::getOriginalDate, date));
            if (existing > 0) continue;

            CalendarEvent instance = new CalendarEvent();
            instance.setUserId(userId);
            instance.setTitle(parent.getTitle());
            instance.setDescription(parent.getDescription());
            instance.setStartTime(date.atTime(parent.getStartTime().toLocalTime()));
            instance.setEndTime(date.atTime(parent.getEndTime().toLocalTime()));
            instance.setAllDay(parent.getAllDay());
            instance.setLocation(parent.getLocation());
            instance.setCategory(parent.getCategory());
            instance.setColor(parent.getColor());
            instance.setPriority(parent.getPriority());
            instance.setStatus("active");
            instance.setSource(parent.getSource());
            instance.setIsRecurring(false);
            instance.setParentEventId(eventId);
            instance.setOriginalDate(date);
            eventMapper.insert(instance);
            count++;
        }

        return count;
    }

    private List<LocalDate> calculateDates(RepeatRule rule, LocalDate eventStart, LocalDate rangeStart, LocalDate rangeEnd) {
        List<LocalDate> dates = new ArrayList<>();
        int interval = rule.getIntervalValue() != null ? rule.getIntervalValue() : 1;

        switch (rule.getRuleType()) {
            case "daily":
                for (LocalDate d = eventStart; !d.isAfter(rangeEnd); d = d.plusDays(interval)) {
                    dates.add(d);
                }
                break;
            case "weekly":
                List<Integer> daysOfWeek = new ArrayList<>();
                if (rule.getDaysOfWeek() != null) {
                    for (String s : rule.getDaysOfWeek().split(",")) {
                        daysOfWeek.add(Integer.parseInt(s.trim()));
                    }
                }
                for (LocalDate d = eventStart; !d.isAfter(rangeEnd); d = d.plusWeeks(interval)) {
                    if (daysOfWeek.isEmpty()) {
                        dates.add(d);
                    } else {
                        LocalDate weekStart = d.with(java.time.DayOfWeek.MONDAY);
                        for (int i = 0; i < 7; i++) {
                            LocalDate day = weekStart.plusDays(i);
                            int dow = day.getDayOfWeek().getValue();
                            if (daysOfWeek.contains(dow) && !day.isBefore(rangeStart) && !day.isAfter(rangeEnd)) {
                                dates.add(day);
                            }
                        }
                    }
                }
                break;
            case "monthly":
                for (LocalDate d = eventStart; !d.isAfter(rangeEnd); d = d.plusMonths(interval)) {
                    dates.add(d);
                }
                break;
            case "yearly":
                for (LocalDate d = eventStart; !d.isAfter(rangeEnd); d = d.plusYears(interval)) {
                    dates.add(d);
                }
                break;
        }

        return dates;
    }

}
