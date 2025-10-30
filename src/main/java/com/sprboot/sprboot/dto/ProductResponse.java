package com.sprboot.sprboot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductResponse {

    private String filename;
    private byte[] fileData;

}
