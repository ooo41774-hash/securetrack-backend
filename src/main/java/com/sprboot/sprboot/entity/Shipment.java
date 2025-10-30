package com.sprboot.sprboot.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "traceabilityHistory", "sender", "receiver", "destination" })
@EqualsAndHashCode(exclude = { "traceabilityHistory", "sender", "receiver", "destination" })
@Entity
@Table(name = "SHIPMENT")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long shipmentID;

    @Column(nullable = false)
    private String status = "pending"; // pending or in transit or received or recalled

    @Column(nullable = false)
    private String qrCodeUrl;

    @Column(nullable = false)
    private LocalDateTime creationTimestamp;

    private LocalDateTime receivedTimestamp;

    private LocalDateTime sentTimestamp;

    private LocalDateTime recalledTimestamp;

    @Column(nullable = false)
    private String HMACHash;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<TraceabilityHistory> traceabilityHistory = new HashSet<>();

    // Foreign Key

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "senderID", nullable = false)
    @JsonBackReference
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiverID", nullable = false)
    @JsonBackReference
    private User receiver;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locationID")
    @JsonBackReference
    private Location destination;
}
