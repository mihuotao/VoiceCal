package com.voicecal.model.vo;

import lombok.Data;

@Data
public class LoginUserVO {

    private String token;
    private String tokenType = "Bearer";
    private long expiresIn;
    private Long userId;
    private String username;
    private String nickname;
    private String lastLoginAt;

}
