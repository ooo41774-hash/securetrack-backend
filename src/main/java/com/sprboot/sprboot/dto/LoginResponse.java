package com.sprboot.sprboot.dto;

import com.sprboot.sprboot.constants.UserRole;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private Long userID;
    private String username;
    private UserRole userRole;

    public LoginResponse(String token, Long userID, String username, UserRole userRole) {
        this.token = token;
        this.userID = userID;
        this.username = username;
        this.userRole = userRole;
    }

}
