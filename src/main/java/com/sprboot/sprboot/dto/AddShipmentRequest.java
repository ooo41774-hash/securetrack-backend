package com.sprboot.sprboot.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class AddShipmentRequest {

    private long senderID;
    private long receiverID;
    private MultipartFile file;
    private String filename;
    private String address;
    private Double latitude;
    private Double longitude;
    private String placeId;

}
