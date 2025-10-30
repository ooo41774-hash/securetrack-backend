package com.sprboot.sprboot.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UnitDTO {
    private String serialNumber;
    private String qrCodeUrl;
    private String description;
    private String status;
    private LocalDateTime createdTimestamp;

    public UnitDTO() {
    }

    public UnitDTO(String serialNumber, String description) {
        this.serialNumber = serialNumber;
        this.description = description;
    }

    public UnitDTO(String serialNumber) {
        this.serialNumber = serialNumber;
    }

}
