package com.voicecal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.voicecal.entity.Reminder;
import com.voicecal.mapper.ReminderMapper;
import com.voicecal.model.dto.CreateReminderRequest;
import com.voicecal.model.dto.UpdateReminderRequest;
import com.voicecal.service.ReminderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderServiceImpl implements ReminderService {

    private final ReminderMapper reminderMapper;

    public ReminderServiceImpl(ReminderMapper reminderMapper) {
        this.reminderMapper = reminderMapper;
    }

    @Override
    public List<Reminder> getByEventId(Long eventId) {
        return reminderMapper.selectList(
                new LambdaQueryWrapper<Reminder>()
                        .eq(Reminder::getEventId, eventId)
                        .orderByAsc(Reminder::getRemindAt));
    }

    @Override
    @Transactional
    public Reminder create(Long eventId, Long userId, CreateReminderRequest req) {
        Reminder reminder = new Reminder();
        reminder.setEventId(eventId);
        reminder.setUserId(userId);
        reminder.setRemindAt(req.getRemindAt());
        reminder.setRemindMinutesBefore(req.getRemindMinutesBefore() != null ? req.getRemindMinutesBefore() : 15);
        reminder.setMethod(req.getMethod() != null ? req.getMethod() : "browser");
        reminder.setIsSent(false);
        reminder.setStatus("pending");
        reminderMapper.insert(reminder);
        return reminder;
    }

    @Override
    @Transactional
    public Reminder update(Long reminderId, Long userId, UpdateReminderRequest req) {
        Reminder reminder = reminderMapper.selectById(reminderId);
        if (reminder == null || !reminder.getUserId().equals(userId)) {
            throw new IllegalArgumentException("提醒不存在或无权操作");
        }
        if (Boolean.TRUE.equals(reminder.getIsSent())) {
            throw new IllegalArgumentException("提醒已发送，无法修改");
        }

        if (req.getRemindAt() != null) reminder.setRemindAt(req.getRemindAt());
        if (req.getRemindMinutesBefore() != null) reminder.setRemindMinutesBefore(req.getRemindMinutesBefore());
        if (req.getMethod() != null) reminder.setMethod(req.getMethod());

        reminderMapper.updateById(reminder);
        return reminder;
    }

    @Override
    @Transactional
    public void delete(Long reminderId, Long userId) {
        Reminder reminder = reminderMapper.selectById(reminderId);
        if (reminder == null || !reminder.getUserId().equals(userId)) {
            throw new IllegalArgumentException("提醒不存在或无权操作");
        }
        reminderMapper.deleteById(reminderId);
    }

    @Override
    public List<Reminder> getPendingBefore(String before) {
        return reminderMapper.selectPendingBefore(before);
    }

    @Override
    @Transactional
    public void markAsSent(Long reminderId) {
        Reminder reminder = reminderMapper.selectById(reminderId);
        if (reminder != null) {
            reminder.setIsSent(true);
            reminder.setSentAt(LocalDateTime.now());
            reminder.setStatus("sent");
            reminderMapper.updateById(reminder);
        }
    }

}
