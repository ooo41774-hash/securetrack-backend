package com.sprboot.sprboot.dto;

import com.sprboot.sprboot.constants.UserRole;

import lombok.Data;

@Data
public class UserDTO {
    private long userID;
    private String username;
    private UserRole role;
    private String address;
    private String phoneNumber;
    private String email;
    private LocationDTO locationDTO;

    public UserDTO(long userID, String username) {
        this.userID = userID;
        this.username = username;
    }

    public UserDTO(String username, UserRole role) {
        this.username = username;
        this.role = role;
    }

    public UserDTO() {
    }

}
