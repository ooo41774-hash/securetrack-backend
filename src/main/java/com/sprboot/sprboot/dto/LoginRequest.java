package com.sprboot.sprboot.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
