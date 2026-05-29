package com.voicecal.scheduled;

import com.voicecal.entity.Reminder;
import com.voicecal.service.AuditLogService;
import com.voicecal.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReminderScanner {

    private static final Logger log = LoggerFactory.getLogger(ReminderScanner.class);

    private final ReminderService reminderService;
    private final AuditLogService auditLogService;

    public ReminderScanner(ReminderService reminderService, AuditLogService auditLogService) {
        this.reminderService = reminderService;
        this.auditLogService = auditLogService;
    }

    @Scheduled(fixedRate = 60000)
    public void scanPendingReminders() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<Reminder> pending = reminderService.getPendingBefore(now);

        if (pending.isEmpty()) {
            return;
        }

        log.debug("提醒扫描: 发现 {} 条待发送提醒", pending.size());

        for (Reminder reminder : pending) {
            try {
                reminderService.markAsSent(reminder.getId());

                auditLogService.log(
                        reminder.getUserId(),
                        "REMINDER_SEND",
                        "Reminder",
                        reminder.getId(),
                        String.format("事件提醒: %s (提前 %d 分钟)",
                                reminder.getRemindAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                                reminder.getRemindMinutesBefore()),
                        "system",
                        "scheduler"
                );

                log.info("提醒已发送: reminderId={}, eventId={}, userId={}",
                        reminder.getId(), reminder.getEventId(), reminder.getUserId());
            } catch (Exception e) {
                log.error("提醒发送失败: reminderId={}", reminder.getId(), e);
            }
        }
    }

}
