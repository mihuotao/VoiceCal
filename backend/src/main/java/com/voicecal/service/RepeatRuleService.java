package com.voicecal.service;

import com.voicecal.entity.RepeatRule;
import com.voicecal.model.dto.CreateRepeatRuleRequest;
import com.voicecal.model.dto.UpdateRepeatRuleRequest;

public interface RepeatRuleService {

    RepeatRule getByEventId(Long eventId);

    RepeatRule create(Long eventId, Long userId, CreateRepeatRuleRequest req);

    RepeatRule update(Long eventId, Long userId, UpdateRepeatRuleRequest req);

    void delete(Long eventId, Long userId);

    int generateInstances(Long eventId, Long userId, String startDate, String endDate);

}
