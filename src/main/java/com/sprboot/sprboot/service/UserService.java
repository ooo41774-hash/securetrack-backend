package com.sprboot.sprboot.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sprboot.sprboot.dto.ChangePasswordRequest;
import com.sprboot.sprboot.dto.LocationDTO;
import com.sprboot.sprboot.dto.SignupRequest;
import com.sprboot.sprboot.dto.UpdateProfileRequest;
import com.sprboot.sprboot.dto.UserDTO;
import com.sprboot.sprboot.entity.Location;
import com.sprboot.sprboot.entity.User;
import com.sprboot.sprboot.repository.LocationRepository;
import com.sprboot.sprboot.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private BCryptPasswordEncoder encoder;

    public UserService(UserRepository userRepository, LocationRepository locationRepository) {
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
    }

    @PostConstruct
    private void init() {
        encoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public User signup(SignupRequest addUserRequest) {

        if (userRepository.findByUsername(addUserRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists!");
        }

        Location location = new Location();
        location.setAddress(addUserRequest.getAddress());
        location.setLatitude(addUserRequest.getLatitude());
        location.setLongitude(addUserRequest.getLongitude());
        location.setPlaceId(addUserRequest.getPlaceId());
        Location savedLocation = locationRepository.save(location);

        String hashedPassword = encoder.encode(addUserRequest.getPassword());

        User user = new User();
        user.setUsername(addUserRequest.getUsername());
        user.setRole(addUserRequest.getUserRole());
        user.setPassword(hashedPassword);
        user.setEmail(addUserRequest.getEmail());
        user.setPhoneNumber(addUserRequest.getPhoneNumber());
        user.setLocation(savedLocation);

        return userRepository.save(user);

    }

    public List<UserDTO> searchUser(String username) {
        List<User> users = userRepository.findByUsernameContainingIgnoreCase(username);
        // System.out.println("Users: " + users);
        return users.stream()
                .map(u -> {
                    UserDTO uDto = new UserDTO(
                            u.getUserID(),
                            u.getUsername());

                    Location location = u.getLocation();
                    LocationDTO lDto = new LocationDTO();
                    lDto.setAddress(location.getAddress());
                    lDto.setLatitude(location.getLatitude());
                    lDto.setLongitude(location.getLongitude());
                    lDto.setPlaceId(location.getPlaceId());
                    uDto.setLocationDTO(lDto);
                    return uDto;
                })
                .toList();
    }

    public UserDTO getProfileInfo(Long userID) {

        User user = userRepository.findByUserID(userID)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));
        UserDTO uDto = new UserDTO();
        uDto.setUsername(user.getUsername());
        uDto.setPhoneNumber(user.getPhoneNumber());
        uDto.setEmail(user.getEmail());

        LocationDTO lDto = new LocationDTO();
        lDto.setAddress(user.getLocation().getAddress());
        lDto.setLatitude(user.getLocation().getLatitude());
        lDto.setLongitude(user.getLocation().getLongitude());
        lDto.setPlaceId(user.getLocation().getPlaceId());
        uDto.setLocationDTO(lDto);
        return uDto;
    }

    public boolean updateProfile(UpdateProfileRequest uReq) {

        try {
            userRepository.updateProfile(
                    uReq.getPhoneNumber(),
                    uReq.getEmail(),
                    uReq.getAddress(),
                    uReq.getUserRole(),
                    uReq.getPlaceId(),
                    uReq.getLatitude(),
                    uReq.getLongitude(),
                    uReq.getUserID());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Update Profile Error: " + e.getMessage());
        }
    }

    public boolean changePassword(ChangePasswordRequest request) {
        try {

            User user = userRepository.findById(request.getUserID())
                    .orElseThrow(() -> new IllegalArgumentException("User Not Found!"));
            System.out.println("Checking if " + request.getCurrentPassword() + " matches stored hash");
            System.out.println("Match result: " + encoder.matches(request.getCurrentPassword(), user.getPassword()));
            if (encoder.matches(request.getCurrentPassword(), user.getPassword())) {
                userRepository.updatePassword(encoder.encode(request.getNewPassword()), request.getUserID());
            } else {
                throw new IllegalArgumentException("Old Password Incorrect!");
            }

            return true;
        } catch (Exception e) {
            System.out.println("Change password Error: " + e.getMessage());
            throw new IllegalArgumentException("Change Password Error: " + e.getMessage());
        }
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
