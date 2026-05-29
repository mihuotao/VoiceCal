package com.voicecal.service;

import com.voicecal.entity.User;

import java.util.List;

public interface UserService {

    User getById(Long id);

    User getByUsername(String username);

    boolean checkUsernameExists(String username);

    boolean checkEmailExists(String email);

    boolean checkEmailExistsExcludeId(String email, Long excludeId);

    Long register(String username, String password, String nickname, String email, String phone);

    void updateLastLogin(Long userId);

    void updateUser(Long userId, String nickname, String email, String phone, String avatar);

    void changePassword(Long userId, String oldPassword, String newPassword);

    List<User> listAll();

}
