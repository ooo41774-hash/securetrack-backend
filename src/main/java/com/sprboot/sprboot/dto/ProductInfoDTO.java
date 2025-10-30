package com.sprboot.sprboot.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ProductInfoDTO {
    private Long productID;
    private String productCode;
    private String productName;
    private Long batchCount;
    private Long unitCount;
    private LocalDate createdDate;

    public ProductInfoDTO(Long productID, String productCode, String productName, Long batchCount, Long unitCount,
            LocalDate createdDate) {
        this.productID = productID;
        this.productCode = productCode;
        this.productName = productName;
        this.batchCount = batchCount;
        this.unitCount = unitCount;
        this.createdDate = createdDate;
    }

    public ProductInfoDTO() {
    }
}
