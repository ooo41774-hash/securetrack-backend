package com.sprboot.sprboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sprboot.sprboot.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

}
