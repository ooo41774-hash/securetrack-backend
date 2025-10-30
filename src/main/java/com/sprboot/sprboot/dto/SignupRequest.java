package com.sprboot.sprboot.dto;

import com.sprboot.sprboot.constants.UserRole;

import lombok.Data;

@Data
public class SignupRequest {

    private String username;
    private UserRole userRole;
    private String phoneNumber;
    private String email;
    private String password;
    private String placeId;
    private String address;
    private Double longitude;
    private Double latitude;
}
