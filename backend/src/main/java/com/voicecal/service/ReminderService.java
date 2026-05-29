package com.voicecal.service;

import com.voicecal.entity.Reminder;
import com.voicecal.model.dto.CreateReminderRequest;
import com.voicecal.model.dto.UpdateReminderRequest;

import java.util.List;

public interface ReminderService {

    List<Reminder> getByEventId(Long eventId);

    Reminder create(Long eventId, Long userId, CreateReminderRequest req);

    Reminder update(Long reminderId, Long userId, UpdateReminderRequest req);

    void delete(Long reminderId, Long userId);

    List<Reminder> getPendingBefore(String before);

    void markAsSent(Long reminderId);

}
