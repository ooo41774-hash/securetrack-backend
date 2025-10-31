package com.sprboot.sprboot.dto;

import java.util.List;

import lombok.Data;

@Data
public class BatchDTO {
    private String batchNumber;
    private List<UnitDTO> units;
    private UnitDTO unit;

    public BatchDTO() {
    }

    public BatchDTO(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public BatchDTO(String batchNumber, List<UnitDTO> units) {
        this.batchNumber = batchNumber;
        this.units = units;
    }

    public BatchDTO(String batchNumber, UnitDTO unit) {
        this.batchNumber = batchNumber;
        this.unit = unit;
    }

}
