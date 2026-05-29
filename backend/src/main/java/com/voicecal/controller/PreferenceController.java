package com.voicecal.controller;

import com.voicecal.auth.CurrentUser;
import com.voicecal.auth.LoginUser;
import com.voicecal.common.ApiResult;
import com.voicecal.entity.UserPreference;
import com.voicecal.model.dto.UpdatePreferenceRequest;
import com.voicecal.service.UserPreferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/preferences")
public class PreferenceController {

    private final UserPreferenceService userPreferenceService;

    public PreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @GetMapping
    public ApiResult<UserPreference> get(@CurrentUser LoginUser loginUser) {
        return ApiResult.success(userPreferenceService.getByUserId(loginUser.getUserId()));
    }

    @PatchMapping
    public ApiResult<UserPreference> update(@CurrentUser LoginUser loginUser,
                                             @RequestBody UpdatePreferenceRequest req) {
        return ApiResult.success(userPreferenceService.updateByUserId(loginUser.getUserId(), req));
    }

}
