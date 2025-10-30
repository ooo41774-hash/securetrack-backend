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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "batch", "traceabilityHistory" })
@EqualsAndHashCode(exclude = { "batch", "traceabilityHistory" })
@Entity
@Table(name = "UNIT")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long unitID;

    @Column(nullable = false)
    private String serialNumber;

    private String description = "";

    @Column(nullable = false)
    private LocalDateTime createdTimestamp;

    @Column(nullable = false)
    private String HMACHash;

    @Column(nullable = false, length = 20)
    private String status = "created"; // created or pending or shipped or received

    @Column(nullable = false)
    private String qrCodeURL;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column(nullable = false)
    private long currentCustodianID;

    @OneToMany(mappedBy = "unit", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<TraceabilityHistory> traceabilityHistory = new HashSet<>();

    // Foreign Key

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batchID", nullable = false)
    @JsonBackReference
    private Batch batch;
}
