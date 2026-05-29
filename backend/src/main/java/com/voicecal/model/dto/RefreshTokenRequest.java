package com.voicecal.model.dto;

import jakarta.validation.constraints.NotBlank;

public class RefreshTokenRequest {

    @NotBlank(message = "Token 不能为空")
    private String token;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

}
