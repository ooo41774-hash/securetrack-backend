package com.sprboot.sprboot.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class CustodySummaryDTO {
    private String productCode;
    private String productName;
    private String batchNumber;
    private LocalDate createdDate;
    private String status;
    private String serialNumber;
    private String description;

    public CustodySummaryDTO() {
    }
}
