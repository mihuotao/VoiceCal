package com.voicecal.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.voicecal.entity.User;
import com.voicecal.mapper.UserMapper;
import com.voicecal.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username));
    }

    @Override
    public boolean checkUsernameExists(String username) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)) > 0;
    }

    @Override
    public boolean checkEmailExists(String email) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)) > 0;
    }

    @Override
    public Long register(String username, String password, String nickname, String email, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public void updateLastLogin(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

}
