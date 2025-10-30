package com.sprboot.sprboot.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "shipment", "unit" })
@EqualsAndHashCode(exclude = { "shipment", "unit" })
@Entity
@Table(name = "TRACEABILITYHISTORY")
public class TraceabilityHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long historyID;

    @Column(nullable = false)
    private String status = "hold"; // or completed

    @ManyToOne
    @JoinColumn(name = "shipmentID", nullable = false)
    @JsonBackReference
    private Shipment shipment;

    @ManyToOne
    @JoinColumn(name = "unitID", nullable = false)
    @JsonBackReference
    private Unit unit;
}
