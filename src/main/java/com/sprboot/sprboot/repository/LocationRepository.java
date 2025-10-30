package com.sprboot.sprboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sprboot.sprboot.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {

}
