package com.voicecal.model.vo;

import lombok.Data;

@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private String lastLoginAt;
    private String createdAt;

}
