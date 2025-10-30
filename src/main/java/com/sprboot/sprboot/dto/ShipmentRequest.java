package com.sprboot.sprboot.dto;

import lombok.Data;

@Data
public class ShipmentRequest {
    private Long userID;
    private Long shipmentID;
    private Double latitude;
    private Double longitude;
}
