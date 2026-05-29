package com.voicecal.controller;

import com.voicecal.auth.CurrentUser;
import com.voicecal.auth.LoginUser;
import com.voicecal.common.ApiResult;
import com.voicecal.common.ResultCode;
import com.voicecal.entity.CalendarEvent;
import com.voicecal.model.dto.BatchDeleteRequest;
import com.voicecal.model.dto.CreateEventRequest;
import com.voicecal.model.dto.EventQuery;
import com.voicecal.model.dto.UpdateEventRequest;
import com.voicecal.model.dto.UpdateStatusRequest;
import com.voicecal.model.vo.CalendarViewVO;
import com.voicecal.model.vo.EventVO;
import com.voicecal.service.EventService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ApiResult<EventVO> create(@CurrentUser LoginUser loginUser,
                                      @Valid @RequestBody CreateEventRequest req) {
        CalendarEvent event = eventService.create(loginUser.getUserId(), req);
        return ApiResult.created(toEventVO(event));
    }

    @GetMapping("/{eventId}")
    public ApiResult<EventVO> getById(@PathVariable Long eventId) {
        CalendarEvent event = eventService.getById(eventId);
        if (event == null) {
            return ApiResult.notFound("事件不存在");
        }
        return ApiResult.success(toEventVO(event));
    }

    @GetMapping
    public ApiResult<List<EventVO>> list(@CurrentUser LoginUser loginUser,
                                          EventQuery query,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        var pageResult = eventService.listByQuery(loginUser.getUserId(), query, page, size);
        List<EventVO> list = pageResult.getRecords().stream()
                .map(this::toEventVO).collect(Collectors.toList());
        return ApiResult.page(list, pageResult.getTotal(), page, size);
    }

    @GetMapping("/calendar")
    public ApiResult<CalendarViewVO> calendar(@CurrentUser LoginUser loginUser,
                                               @RequestParam(defaultValue = "month") String view,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        CalendarViewVO result = eventService.getCalendarView(loginUser.getUserId(), view, date);
        return ApiResult.success(result);
    }

    @PutMapping("/{eventId}")
    public ApiResult<EventVO> update(@PathVariable Long eventId,
                                      @CurrentUser LoginUser loginUser,
                                      @Valid @RequestBody UpdateEventRequest req) {
        try {
            CalendarEvent event = eventService.update(eventId, loginUser.getUserId(), req);
            return ApiResult.success(toEventVO(event));
        } catch (IllegalArgumentException e) {
            return ApiResult.error(ResultCode.FORBIDDEN.getCode(), e.getMessage());
        }
    }

    @PatchMapping("/{eventId}")
    public ApiResult<EventVO> patch(@PathVariable Long eventId,
                                     @CurrentUser LoginUser loginUser,
                                     @Valid @RequestBody UpdateEventRequest req) {
        return update(eventId, loginUser, req);
    }

    @DeleteMapping("/{eventId}")
    public ApiResult<Void> delete(@PathVariable Long eventId,
                                   @CurrentUser LoginUser loginUser,
                                   @RequestParam(defaultValue = "true") boolean deleteRule) {
        try {
            eventService.delete(eventId, loginUser.getUserId(), deleteRule);
            return ApiResult.noContent();
        } catch (IllegalArgumentException e) {
            return ApiResult.error(ResultCode.FORBIDDEN.getCode(), e.getMessage());
        }
    }

    @PostMapping("/batch-delete")
    public ApiResult<Void> batchDelete(@CurrentUser LoginUser loginUser,
                                        @Valid @RequestBody BatchDeleteRequest req) {
        eventService.batchDelete(req.getIds(), loginUser.getUserId());
        return ApiResult.success();
    }

    @PatchMapping("/{eventId}/status")
    public ApiResult<Void> updateStatus(@PathVariable Long eventId,
                                         @CurrentUser LoginUser loginUser,
                                         @Valid @RequestBody UpdateStatusRequest req) {
        try {
            eventService.updateStatus(eventId, loginUser.getUserId(), req.getStatus());
            return ApiResult.success();
        } catch (IllegalArgumentException e) {
            return ApiResult.error(ResultCode.FORBIDDEN.getCode(), e.getMessage());
        }
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
