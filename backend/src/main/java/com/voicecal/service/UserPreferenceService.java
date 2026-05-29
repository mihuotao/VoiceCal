package com.voicecal.service;

import com.voicecal.entity.UserPreference;
import com.voicecal.model.dto.UpdatePreferenceRequest;

public interface UserPreferenceService {

    UserPreference getByUserId(Long userId);

    UserPreference updateByUserId(Long userId, UpdatePreferenceRequest req);

}
