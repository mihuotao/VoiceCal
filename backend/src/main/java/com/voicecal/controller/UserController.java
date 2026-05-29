package com.voicecal.controller;

import com.voicecal.auth.CurrentUser;
import com.voicecal.auth.LoginUser;
import com.voicecal.common.ApiResult;
import com.voicecal.common.ResultCode;
import com.voicecal.entity.User;
import com.voicecal.model.dto.ChangePasswordRequest;
import com.voicecal.model.dto.UpdateUserRequest;
import com.voicecal.model.vo.UserInfoVO;
import com.voicecal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResult<UserInfoVO> getCurrentUser(@CurrentUser LoginUser loginUser) {
        User user = userService.getById(loginUser.getUserId());
        if (user == null) {
            return ApiResult.notFound("用户不存在");
        }
        return ApiResult.success(toUserInfoVO(user));
    }

    @PatchMapping("/me")
    public ApiResult<UserInfoVO> updateCurrentUser(@CurrentUser LoginUser loginUser,
                                                    @Valid @RequestBody UpdateUserRequest req) {
        if (req.getEmail() != null && !req.getEmail().isEmpty()
                && userService.checkEmailExistsExcludeId(req.getEmail(), loginUser.getUserId())) {
            return ApiResult.error(ResultCode.CONFLICT.getCode(), "邮箱已被使用");
        }
        userService.updateUser(loginUser.getUserId(), req.getNickname(),
                req.getEmail(), req.getPhone(), req.getAvatar());
        User user = userService.getById(loginUser.getUserId());
        return ApiResult.success(toUserInfoVO(user));
    }

    @PatchMapping("/me/password")
    public ApiResult<Void> changePassword(@CurrentUser LoginUser loginUser,
                                           @Valid @RequestBody ChangePasswordRequest req) {
        try {
            userService.changePassword(loginUser.getUserId(), req.getOldPassword(), req.getNewPassword());
            return ApiResult.success();
        } catch (IllegalArgumentException e) {
            return ApiResult.badRequest(e.getMessage());
        }
    }

    private UserInfoVO toUserInfoVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setLastLoginAt(user.getLastLoginAt() != null
                ? user.getLastLoginAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : null);
        vo.setCreatedAt(user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : null);
        return vo;
    }

}
