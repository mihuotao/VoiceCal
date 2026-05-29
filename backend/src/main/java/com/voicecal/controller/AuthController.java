package com.voicecal.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.voicecal.auth.JwtProvider;
import com.voicecal.common.ApiResult;
import com.voicecal.common.ResultCode;
import com.voicecal.entity.User;
import com.voicecal.model.dto.LoginRequest;
import com.voicecal.model.dto.RegisterRequest;
import com.voicecal.model.vo.LoginUserVO;
import com.voicecal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtProvider jwtProvider;

    public AuthController(UserService userService, JwtProvider jwtProvider) {
        this.userService = userService;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/register")
    public ApiResult<LoginUserVO> register(@Valid @RequestBody RegisterRequest req) {
        if (userService.checkUsernameExists(req.getUsername())) {
            return ApiResult.error(ResultCode.CONFLICT.getCode(), "用户名已存在");
        }
        if (req.getEmail() != null && !req.getEmail().isEmpty()
                && userService.checkEmailExists(req.getEmail())) {
            return ApiResult.error(ResultCode.CONFLICT.getCode(), "邮箱已被使用");
        }

        String nickname = req.getNickname() != null ? req.getNickname() : req.getUsername();
        Long userId = userService.register(
                req.getUsername(), req.getPassword(),
                nickname, req.getEmail(), req.getPhone());

        String token = jwtProvider.generateToken(userId, req.getUsername());

        LoginUserVO vo = new LoginUserVO();
        vo.setToken(token);
        vo.setExpiresIn(jwtProvider.getExpiration());
        vo.setUserId(userId);
        vo.setUsername(req.getUsername());
        vo.setNickname(nickname);

        return ApiResult.created(vo);
    }

    @PostMapping("/login")
    public ApiResult<LoginUserVO> login(@Valid @RequestBody LoginRequest req) {
        User user = userService.getByUsername(req.getUsername());
        if (user == null) {
            return ApiResult.error(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return ApiResult.error(ResultCode.ACCOUNT_DISABLED);
        }

        if (!BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            return ApiResult.error(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        userService.updateLastLogin(user.getId());

        String token = jwtProvider.generateToken(user.getId(), user.getUsername());

        LoginUserVO vo = new LoginUserVO();
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(jwtProvider.getExpiration());
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setLastLoginAt(user.getLastLoginAt() != null
                ? user.getLastLoginAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : null);

        return ApiResult.success(vo);
    }

}
