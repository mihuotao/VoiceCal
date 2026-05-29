package com.voicecal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.voicecal.entity.UserPreference;
import com.voicecal.mapper.UserPreferenceMapper;
import com.voicecal.model.dto.UpdatePreferenceRequest;
import com.voicecal.service.UserPreferenceService;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceMapper userPreferenceMapper;

    public UserPreferenceServiceImpl(UserPreferenceMapper userPreferenceMapper) {
        this.userPreferenceMapper = userPreferenceMapper;
    }

    @Override
    public UserPreference getByUserId(Long userId) {
        UserPreference pref = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId));
        if (pref == null) {
            pref = createDefault(userId);
        }
        return pref;
    }

    @Override
    public UserPreference updateByUserId(Long userId, UpdatePreferenceRequest req) {
        UserPreference pref = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId));

        if (pref == null) {
            pref = createDefault(userId);
        }

        if (req.getDefaultView() != null) pref.setDefaultView(req.getDefaultView());
        if (req.getDefaultCategory() != null) pref.setDefaultCategory(req.getDefaultCategory());
        if (req.getDefaultReminder() != null) pref.setDefaultReminder(req.getDefaultReminder());
        if (req.getLanguage() != null) pref.setLanguage(req.getLanguage());
        if (req.getWeekStartDay() != null) pref.setWeekStartDay(req.getWeekStartDay());
        if (req.getWorkingHoursStart() != null) pref.setWorkingHoursStart(LocalTime.parse(req.getWorkingHoursStart()));
        if (req.getWorkingHoursEnd() != null) pref.setWorkingHoursEnd(LocalTime.parse(req.getWorkingHoursEnd()));
        if (req.getTtsEnabled() != null) pref.setTtsEnabled(req.getTtsEnabled());
        if (req.getTtsVoiceType() != null) pref.setTtsVoiceType(req.getTtsVoiceType());
        if (req.getTtsSpeed() != null) pref.setTtsSpeed(req.getTtsSpeed());
        if (req.getNotificationEnabled() != null) pref.setNotificationEnabled(req.getNotificationEnabled());
        if (req.getTheme() != null) pref.setTheme(req.getTheme());

        userPreferenceMapper.updateById(pref);
        return pref;
    }

    private UserPreference createDefault(Long userId) {
        UserPreference pref = new UserPreference();
        pref.setUserId(userId);
        pref.setDefaultView("month");
        pref.setDefaultCategory("personal");
        pref.setDefaultReminder(15);
        pref.setLanguage("zh-CN");
        pref.setWeekStartDay(1);
        pref.setWorkingHoursStart(LocalTime.of(9, 0));
        pref.setWorkingHoursEnd(LocalTime.of(18, 0));
        pref.setTtsEnabled(true);
        pref.setTtsVoiceType("female");
        pref.setTtsSpeed(5);
        pref.setNotificationEnabled(true);
        pref.setTheme("light");
        userPreferenceMapper.insert(pref);
        return pref;
    }

}
