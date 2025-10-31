package com.sprboot.sprboot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sprboot.sprboot.constants.UserRole;
import com.sprboot.sprboot.entity.User;

import jakarta.transaction.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByUsernameContainingIgnoreCase(String username);

    Optional<User> findByUsername(String username);

    Optional<User> findByUserID(Long userID);

    @Transactional
    @Modifying
    @Query("""
                UPDATE User u
                SET
                    u.phoneNumber = :phoneNumber,
                    u.email = :email,
                    u.location.address = :address,
                    u.role = :role,
                    u.location.latitude = :latitude,
                    u.location.longitude = :longitude,
                    u.location.placeId = :placeId
                WHERE u.userID = :userID
            """)
    void updateProfile(
            @Param("phoneNumber") String phoneNumber,
            @Param("email") String email,
            @Param("address") String address,
            @Param("role") UserRole role,
            @Param("placeId") String placeId,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("userID") Long userID);

    @Transactional
    @Modifying
    @Query("""
                UPDATE User u
                SET
                    u.password = :password
                WHERE u.userID = :userID
            """)
    void updatePassword(
            @Param("password") String password,
            @Param("userID") Long userID);
}
