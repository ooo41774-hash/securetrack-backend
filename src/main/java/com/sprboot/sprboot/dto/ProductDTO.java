package com.sprboot.sprboot.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductDTO {
    private String productCode;
    private String productName;
    private UserDTO user;
    private LocalDate createdDate;
    private List<BatchDTO> batches;
    private BatchDTO batch;

    public ProductDTO(String productCode, String productName) {
        this.productCode = productCode;
        this.productName = productName;
    }

    public ProductDTO() {
    }

    public ProductDTO(String productCode, String productName, List<BatchDTO> batches) {
        this.productCode = productCode;
        this.productName = productName;
        this.batches = batches;
    }

    public ProductDTO(String productCode, String productName, BatchDTO batch) {
        this.productCode = productCode;
        this.productName = productName;
        this.batch = batch;
    }

}
