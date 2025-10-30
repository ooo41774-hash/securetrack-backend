package com.sprboot.sprboot.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class ShipmentDTO {

    private Long shipmentID;
    private String senderUsername;
    private String receiverUsername;
    private LocalDateTime creationTimestamp;
    private LocalDateTime sentTimestamp;
    private LocalDateTime receivedTimestamp;
    private String senderAddress;
    private String receiverAddress;
    private String status;
    private String qrCodeUrl;
    private String shipmentAddress;
    private List<CustodyInfoDTO> custodyInfoDTOs;

    public ShipmentDTO(LocalDateTime receivedTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
    }

    public ShipmentDTO() {
    }

}
