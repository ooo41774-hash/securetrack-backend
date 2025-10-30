package com.sprboot.sprboot.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CustodyInfoDTO {

    private String productCode;
    private String productName;
    private String batchNumber;
    private String serialNumber;
    private String description;
    private String status; // created || pending shipment || shipped || received
    private String receivedFrom;
    private String transferredTo;
    private LocalDateTime createdReceivedDate;
    private String qrCodeUrl;

    public CustodyInfoDTO() {
    }

    public CustodyInfoDTO(String productCode, String productName, String batchNumber, String serialNumber,
            String description, String status, String receivedFrom, String transferredTo,
            LocalDateTime createdTimestamp) {
        this.productCode = productCode;
        this.productName = productName;
        this.batchNumber = batchNumber;
        this.serialNumber = serialNumber;
        this.description = description;
        this.status = status;
        this.receivedFrom = receivedFrom;
        this.transferredTo = transferredTo;
        this.createdReceivedDate = createdTimestamp;
    }

    public void setStatus(String status) {
        if (status.equals("pending")) {
            status = "pending shipment";
        } else if (status.equals("created")) {
            status = "in custody";
        }
        this.status = status;
    }

}
