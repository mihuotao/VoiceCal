package com.voicecal.service;

import com.voicecal.entity.User;

public interface UserService {

    User getById(Long id);

    User getByUsername(String username);

    boolean checkUsernameExists(String username);

    boolean checkEmailExists(String email);

    Long register(String username, String password, String nickname, String email, String phone);

    void updateLastLogin(Long userId);

}
