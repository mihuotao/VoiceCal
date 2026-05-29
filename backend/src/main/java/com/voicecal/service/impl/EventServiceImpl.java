package com.voicecal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voicecal.entity.CalendarEvent;
import com.voicecal.entity.Reminder;
import com.voicecal.entity.RepeatRule;
import com.voicecal.mapper.CalendarEventMapper;
import com.voicecal.mapper.ReminderMapper;
import com.voicecal.mapper.RepeatRuleMapper;
import com.voicecal.model.dto.CreateEventRequest;
import com.voicecal.model.dto.EventQuery;
import com.voicecal.model.dto.UpdateEventRequest;
import com.voicecal.model.vo.CalendarViewVO;
import com.voicecal.model.vo.EventVO;
import com.voicecal.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final CalendarEventMapper eventMapper;
    private final RepeatRuleMapper repeatRuleMapper;
    private final ReminderMapper reminderMapper;

    public EventServiceImpl(CalendarEventMapper eventMapper,
                            RepeatRuleMapper repeatRuleMapper,
                            ReminderMapper reminderMapper) {
        this.eventMapper = eventMapper;
        this.repeatRuleMapper = repeatRuleMapper;
        this.reminderMapper = reminderMapper;
    }

    @Override
    @Transactional
    public CalendarEvent create(Long userId, CreateEventRequest req) {
        CalendarEvent event = new CalendarEvent();
        event.setUserId(userId);
        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setStartTime(req.getStartTime());
        event.setEndTime(req.getEndTime());
        event.setAllDay(req.getAllDay() != null ? req.getAllDay() : false);
        event.setLocation(req.getLocation());
        event.setCategory(req.getCategory() != null ? req.getCategory() : "personal");
        event.setColor(req.getColor() != null ? req.getColor() : "#409EFF");
        event.setPriority(req.getPriority() != null ? req.getPriority() : 5);
        event.setStatus("active");
        event.setSource("manual");
        event.setIsRecurring(false);

        eventMapper.insert(event);

        if (req.getRepeatRule() != null) {
            CreateEventRequest.RepeatRuleRequest rr = req.getRepeatRule();
            RepeatRule rule = new RepeatRule();
            rule.setEventId(event.getId());
            rule.setRuleType(rr.getRuleType());
            rule.setIntervalValue(rr.getIntervalValue() != null ? rr.getIntervalValue() : 1);
            rule.setDaysOfWeek(rr.getDaysOfWeek());
            rule.setDaysOfMonth(rr.getDaysOfMonth());
            rule.setMonthlyType(rr.getMonthlyType());
            rule.setWeekOfMonth(rr.getWeekOfMonth());
            rule.setEndType(rr.getEndType() != null ? rr.getEndType() : "never");
            rule.setEndCount(rr.getEndCount());
            rule.setEndDate(rr.getEndDate());
            repeatRuleMapper.insert(rule);
            event.setIsRecurring(true);
            eventMapper.updateById(event);
        }

        if (req.getReminders() != null) {
            for (CreateEventRequest.ReminderRequest r : req.getReminders()) {
                int minutesBefore = r.getRemindMinutesBefore() != null ? r.getRemindMinutesBefore() : 15;
                Reminder reminder = new Reminder();
                reminder.setEventId(event.getId());
                reminder.setUserId(userId);
                reminder.setRemindAt(req.getStartTime().minusMinutes(minutesBefore));
                reminder.setRemindMinutesBefore(minutesBefore);
                reminder.setMethod(r.getMethod() != null ? r.getMethod() : "browser");
                reminder.setIsSent(false);
                reminder.setStatus("pending");
                reminderMapper.insert(reminder);
            }
        }

        return event;
    }

    @Override
    public CalendarEvent getById(Long eventId) {
        return eventMapper.selectById(eventId);
    }

    @Override
    public Page<CalendarEvent> listByQuery(Long userId, EventQuery query, int page, int size) {
        Page<CalendarEvent> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<CalendarEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalendarEvent::getUserId, userId);

        if (query.getStartDate() != null) {
            wrapper.ge(CalendarEvent::getStartTime, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.le(CalendarEvent::getStartTime, query.getEndDate().atTime(23, 59, 59));
        }
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(CalendarEvent::getCategory, query.getCategory());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(CalendarEvent::getStatus, query.getStatus());
        }
        if (query.getPriority() != null) {
            wrapper.ge(CalendarEvent::getPriority, query.getPriority());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(CalendarEvent::getTitle, query.getKeyword())
                    .or().like(CalendarEvent::getDescription, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getSource())) {
            wrapper.eq(CalendarEvent::getSource, query.getSource());
        }

        wrapper.orderByAsc(CalendarEvent::getStartTime);
        return eventMapper.selectPage(mpPage, wrapper);
    }

    @Override
    @Transactional
    public CalendarEvent update(Long eventId, Long userId, UpdateEventRequest req) {
        CalendarEvent existing = eventMapper.selectById(eventId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("事件不存在或无权操作");
        }

        if (StringUtils.hasText(req.getTitle())) {
            existing.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            existing.setDescription(req.getDescription());
        }
        if (req.getStartTime() != null) {
            existing.setStartTime(req.getStartTime());
        }
        if (req.getEndTime() != null) {
            existing.setEndTime(req.getEndTime());
        }
        if (req.getAllDay() != null) {
            existing.setAllDay(req.getAllDay());
        }
        if (req.getLocation() != null) {
            existing.setLocation(req.getLocation());
        }
        if (req.getCategory() != null) {
            existing.setCategory(req.getCategory());
        }
        if (req.getColor() != null) {
            existing.setColor(req.getColor());
        }
        if (req.getPriority() != null) {
            existing.setPriority(req.getPriority());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus());
        }

        eventMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void delete(Long eventId, Long userId, boolean deleteRule) {
        CalendarEvent existing = eventMapper.selectById(eventId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new IllegalArgumentException("事件不存在或无权操作");
        }
        if (deleteRule) {
            repeatRuleMapper.delete(new LambdaQueryWrapper<RepeatRule>().eq(RepeatRule::getEventId, eventId));
        }
        reminderMapper.delete(new LambdaQueryWrapper<Reminder>().eq(Reminder::getEventId, eventId));
        eventMapper.deleteById(eventId);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids, Long userId) {
        for (Long id : ids) {
            delete(id, userId, true);
        }
    }

    @Override
    @Transactional
    public void updateStatus(Long eventId, Long userId, String status) {
        CalendarEvent event = eventMapper.selectById(eventId);
        if (event == null || !event.getUserId().equals(userId)) {
            throw new IllegalArgumentException("事件不存在或无权操作");
        }
        event.setStatus(status);
        eventMapper.updateById(event);
    }

    @Override
    public CalendarViewVO getCalendarView(Long userId, String view, LocalDate date) {
        CalendarViewVO result = new CalendarViewVO();
        result.setView(view);
        result.setCurrentDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE));

        LocalDate start, end;
        switch (view) {
            case "week":
                start = date.with(java.time.DayOfWeek.MONDAY);
                end = start.plusDays(6);
                break;
            case "day":
                start = date;
                end = date;
                break;
            default:
                start = date.withDayOfMonth(1);
                end = date.withDayOfMonth(date.lengthOfMonth());
                break;
        }

        List<CalendarEvent> events = listByDateRange(userId, start, end);

        List<CalendarViewVO.CalendarDayVO> days = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            CalendarViewVO.CalendarDayVO day = new CalendarViewVO.CalendarDayVO();
            day.setDate(cursor.format(DateTimeFormatter.ISO_LOCAL_DATE));
            day.setIsCurrentMonth(cursor.getMonth() == date.getMonth());
            day.setIsToday(cursor.equals(LocalDate.now()));

            LocalDate finalCursor = cursor;
            List<EventVO> dayEvents = events.stream()
                    .filter(e -> {
                        LocalDate eStart = e.getStartTime().toLocalDate();
                        LocalDate eEnd = e.getEndTime().toLocalDate();
                        return !finalCursor.isBefore(eStart) && !finalCursor.isAfter(eEnd);
                    })
                    .map(this::toEventVO)
                    .collect(Collectors.toList());
            day.setEvents(dayEvents);

            days.add(day);
            cursor = cursor.plusDays(1);
        }

        result.setDays(days);
        return result;
    }

    @Override
    public List<CalendarEvent> listByDateRange(Long userId, LocalDate start, LocalDate end) {
        return eventMapper.selectList(
                new LambdaQueryWrapper<CalendarEvent>()
                        .eq(CalendarEvent::getUserId, userId)
                        .le(CalendarEvent::getStartTime, end.atTime(23, 59, 59))
                        .ge(CalendarEvent::getEndTime, start.atStartOfDay())
                        .orderByAsc(CalendarEvent::getStartTime));
    }

    private EventVO toEventVO(CalendarEvent event) {
        EventVO vo = new EventVO();
        vo.setId(event.getId());
        vo.setUserId(event.getUserId());
        vo.setTitle(event.getTitle());
        vo.setDescription(event.getDescription());
        vo.setStartTime(event.getStartTime() != null
                ? event.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        vo.setEndTime(event.getEndTime() != null
                ? event.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        vo.setAllDay(event.getAllDay());
        vo.setLocation(event.getLocation());
        vo.setCategory(event.getCategory());
        vo.setColor(event.getColor());
        vo.setPriority(event.getPriority());
        vo.setStatus(event.getStatus());
        vo.setSource(event.getSource());
        vo.setIsRecurring(event.getIsRecurring());
        vo.setCreatedAt(event.getCreatedAt() != null
                ? event.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        vo.setUpdatedAt(event.getUpdatedAt() != null
                ? event.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        return vo;
    }

}
