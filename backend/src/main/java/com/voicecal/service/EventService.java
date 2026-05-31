package com.voicecal.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.voicecal.entity.CalendarEvent;
import com.voicecal.model.dto.CreateEventRequest;
import com.voicecal.model.dto.EventQuery;
import com.voicecal.model.dto.UpdateEventRequest;
import com.voicecal.model.vo.CalendarViewVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    /**
     * 检测时间冲突
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 冲突的事件列表
     */
    List<CalendarEvent> findConflicts(Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 按标题和日期查询事件
     * @param userId 用户ID
     * @param title 事件标题（模糊匹配）
     * @param date 事件日期
     * @return 匹配的事件列表
     */
    List<CalendarEvent> findByTitleAndDate(Long userId, String title, LocalDate date);

}
