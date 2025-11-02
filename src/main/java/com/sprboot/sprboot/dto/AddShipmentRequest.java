package com.sprboot.sprboot.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

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
    private List<String> serialNumbers;

    public void setSerialNumbers(String serialNumbersJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.serialNumbers = mapper.readValue(serialNumbersJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            this.serialNumbers = null;
        }
    }

}
