package com.sprboot.sprboot.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private Long userID;
    private String currentPassword;
    private String newPassword;
}
