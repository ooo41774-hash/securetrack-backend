package com.sprboot.sprboot.dto;

import lombok.Data;

@Data
public class VerifyShipmentRequest {
    private Long userID;
    private String cipherText;
}
