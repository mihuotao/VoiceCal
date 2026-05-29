package com.voicecal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voicecal.entity.CalendarEvent;
import com.voicecal.model.dto.CreateEventRequest;
import com.voicecal.model.dto.EventQuery;
import com.voicecal.model.dto.UpdateEventRequest;
import com.voicecal.model.vo.CalendarViewVO;

import java.time.LocalDate;
import java.util.List;

public interface EventService {

    CalendarEvent create(Long userId, CreateEventRequest req);

    CalendarEvent getById(Long eventId);

    Page<CalendarEvent> listByQuery(Long userId, EventQuery query, int page, int size);

    CalendarEvent update(Long eventId, Long userId, UpdateEventRequest req);

    void delete(Long eventId, Long userId, boolean deleteRule);

    void batchDelete(List<Long> ids, Long userId);

    void updateStatus(Long eventId, Long userId, String status);

    CalendarViewVO getCalendarView(Long userId, String view, LocalDate date);

    List<CalendarEvent> listByDateRange(Long userId, LocalDate start, LocalDate end);

}
