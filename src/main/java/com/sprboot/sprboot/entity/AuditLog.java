package com.sprboot.sprboot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import com.sprboot.sprboot.constants.EventType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "AUDITLOG")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long logID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    private long userID;

    private long unitID;

    private long shipmentID;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
