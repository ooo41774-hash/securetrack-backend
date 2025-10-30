package com.sprboot.sprboot.dto;

import java.util.List;

import lombok.Data;

@Data
public class VerifyProductResponse {

    private String serialNumber;
    private String batchNumber;
    private String productName;
    private String productCode;
    private String description;
    private List<ShipmentDTO> traceRoute;

}
