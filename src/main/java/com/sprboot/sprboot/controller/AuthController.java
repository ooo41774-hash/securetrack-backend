package com.sprboot.sprboot.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import com.sprboot.sprboot.dto.LoginRequest;
import com.sprboot.sprboot.dto.LoginResponse;
import com.sprboot.sprboot.entity.User;
import com.sprboot.sprboot.service.AuthUserDetailsService;
import com.sprboot.sprboot.utility.JwtUtils;

@RestController
@RequestMapping("/api/auth/")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
            UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            User user = ((AuthUserDetailsService) userDetailsService).findByUsername(request.getUsername());

            String token = jwtUtils.generateToken(userDetails.getUsername(), user.getUserID());
            return new LoginResponse(token, user.getUserID(), userDetails.getUsername(), user.getRole());
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Invalid username or password");
        }
    }
}
