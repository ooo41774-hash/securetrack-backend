package com.sprboot.sprboot.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sprboot.sprboot.dto.ChangePasswordRequest;
import com.sprboot.sprboot.dto.SignupRequest;
import com.sprboot.sprboot.dto.UpdateProfileRequest;
import com.sprboot.sprboot.dto.UserDTO;
import com.sprboot.sprboot.entity.User;
import com.sprboot.sprboot.service.UserService;

@RestController
@RequestMapping("/api/user/")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public User signup(@RequestBody SignupRequest signupRequest) {
        return userService.signup(signupRequest);
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUser(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUser(query));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfileInfo(@RequestParam Long userID) {
        return ResponseEntity.ok(userService.getProfileInfo(userID));
    }

    @PostMapping("/profile/update")
    public boolean updateProfile(@RequestBody UpdateProfileRequest updateProfileRequest) {
        return userService.updateProfile(updateProfileRequest);
    }

    @PostMapping("/profile/changePassword")
    public boolean changePassword(@RequestBody ChangePasswordRequest request) {
        return userService.changePassword(request);
    }

}
