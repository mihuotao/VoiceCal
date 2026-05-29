package com.voicecal.scheduled;

import com.voicecal.entity.Festival;
import com.voicecal.entity.FestivalGreetingLog;
import com.voicecal.entity.User;
import com.voicecal.mapper.FestivalGreetingLogMapper;
import com.voicecal.service.AuditLogService;
import com.voicecal.service.FestivalService;
import com.voicecal.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class FestivalGreetingPushTask {

    private static final Logger log = LoggerFactory.getLogger(FestivalGreetingPushTask.class);

    private final FestivalService festivalService;
    private final UserService userService;
    private final FestivalGreetingLogMapper greetingLogMapper;
    private final AuditLogService auditLogService;

    public FestivalGreetingPushTask(FestivalService festivalService,
                                    UserService userService,
                                    FestivalGreetingLogMapper greetingLogMapper,
                                    AuditLogService auditLogService) {
        this.festivalService = festivalService;
        this.userService = userService;
        this.greetingLogMapper = greetingLogMapper;
        this.auditLogService = auditLogService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void pushDailyGreetings() {
        LocalDate today = LocalDate.now();
        var festivals = festivalService.getToday();

        if (festivals.isEmpty()) {
            log.debug("今日无节日，跳过祝福推送");
            return;
        }

        List<User> users = userService.listAll();
        if (users.isEmpty()) {
            return;
        }

        for (var festivalVO : festivals) {
            Festival festival = new Festival();
            festival.setId(festivalVO.getId());
            festival.setName(festivalVO.getName());
            festival.setGreeting(festivalVO.getGreeting());

            for (User user : users) {
                try {
                    FestivalGreetingLog greetingLog = new FestivalGreetingLog();
                    greetingLog.setUserId(user.getId());
                    greetingLog.setFestivalId(festival.getId());
                    greetingLog.setGreetingType("daily_push");
                    greetingLog.setGreetingText(festival.getGreeting());
                    greetingLog.setIsTtsSent(false);
                    greetingLog.setUserAction("pending");
                    greetingLogMapper.insert(greetingLog);

                    auditLogService.log(
                            user.getId(),
                            "FESTIVAL_GREETING_PUSH",
                            "Festival",
                            festival.getId(),
                            String.format("节日祝福: %s - %s", festival.getName(), festival.getGreeting()),
                            "system",
                            "scheduler"
                    );
                } catch (Exception e) {
                    log.error("节日祝福推送失败: userId={}, festivalId={}", user.getId(), festival.getId(), e);
                }
            }

            log.info("节日祝福已推送: festival={}, userCount={}", festival.getName(), users.size());
        }
    }

}
