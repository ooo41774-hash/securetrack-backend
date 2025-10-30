package com.sprboot.sprboot.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ProductRequest {

    private Long registrarID;
    private MultipartFile file;
    private String filename;

}
