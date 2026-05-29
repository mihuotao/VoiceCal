package com.voicecal.controller;

import com.voicecal.auth.CurrentUser;
import com.voicecal.auth.LoginUser;
import com.voicecal.common.ApiResult;
import com.voicecal.common.ResultCode;
import com.voicecal.entity.Reminder;
import com.voicecal.model.dto.CreateReminderRequest;
import com.voicecal.model.dto.UpdateReminderRequest;
import com.voicecal.model.vo.ReminderVO;
import com.voicecal.service.ReminderService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping("/events/{eventId}/reminders")
    public ApiResult<ReminderVO> create(@PathVariable Long eventId,
                                         @CurrentUser LoginUser loginUser,
                                         @Valid @RequestBody CreateReminderRequest req) {
        Reminder reminder = reminderService.create(eventId, loginUser.getUserId(), req);
        return ApiResult.created(toVO(reminder));
    }

    @GetMapping("/events/{eventId}/reminders")
    public ApiResult<List<ReminderVO>> list(@PathVariable Long eventId) {
        List<Reminder> list = reminderService.getByEventId(eventId);
        return ApiResult.success(list.stream().map(this::toVO).collect(Collectors.toList()));
    }

    @PatchMapping("/reminders/{reminderId}")
    public ApiResult<ReminderVO> update(@PathVariable Long reminderId,
                                         @CurrentUser LoginUser loginUser,
                                         @Valid @RequestBody UpdateReminderRequest req) {
        try {
            Reminder reminder = reminderService.update(reminderId, loginUser.getUserId(), req);
            return ApiResult.success(toVO(reminder));
        } catch (IllegalArgumentException e) {
            return ApiResult.error(ResultCode.UNPROCESSABLE_ENTITY.getCode(), e.getMessage());
        }
    }

    @DeleteMapping("/reminders/{reminderId}")
    public ApiResult<Void> delete(@PathVariable Long reminderId,
                                   @CurrentUser LoginUser loginUser) {
        try {
            reminderService.delete(reminderId, loginUser.getUserId());
            return ApiResult.noContent();
        } catch (IllegalArgumentException e) {
            return ApiResult.error(ResultCode.NOT_FOUND.getCode(), e.getMessage());
        }
    }

    @GetMapping("/reminders/pending")
    public ApiResult<List<ReminderVO>> pending(@RequestParam(required = false) String before) {
        String beforeTime = before != null ? before : LocalDateTime.now().toString();
        List<Reminder> list = reminderService.getPendingBefore(beforeTime);
        return ApiResult.success(list.stream().map(this::toVO).collect(Collectors.toList()));
    }

    @PatchMapping("/reminders/{reminderId}/sent")
    public ApiResult<Void> markSent(@PathVariable Long reminderId) {
        reminderService.markAsSent(reminderId);
        return ApiResult.success();
    }

    private ReminderVO toVO(Reminder reminder) {
        ReminderVO vo = new ReminderVO();
        vo.setId(reminder.getId());
        vo.setEventId(reminder.getEventId());
        vo.setRemindAt(reminder.getRemindAt());
        vo.setRemindMinutesBefore(reminder.getRemindMinutesBefore());
        vo.setMethod(reminder.getMethod());
        vo.setIsSent(reminder.getIsSent());
        vo.setSentAt(reminder.getSentAt());
        vo.setStatus(reminder.getStatus());
        vo.setCreatedAt(reminder.getCreatedAt());
        return vo;
    }

}
