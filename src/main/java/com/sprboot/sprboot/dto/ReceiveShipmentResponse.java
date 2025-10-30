package com.sprboot.sprboot.dto;

import lombok.Data;

@Data
public class ReceiveShipmentResponse {
    private int sResult;
    private int thResult;
    private int uResult;

    public ReceiveShipmentResponse(int sResult, int thResult, int uResult) {
        this.sResult = sResult;
        this.thResult = thResult;
        this.uResult = uResult;
    }
}
