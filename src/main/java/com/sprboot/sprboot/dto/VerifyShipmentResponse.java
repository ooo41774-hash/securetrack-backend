package com.sprboot.sprboot.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyShipmentResponse {

    private Long shipmentID;
    private String senderUsername;
    private String receiverUsername;
    private String address;
    private List<ProductDTO> productDTOs;
    private String action;
    private String status;

}
