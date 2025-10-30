package com.sprboot.sprboot.dto;

import lombok.Data;

@Data
public class LocationDTO {
    private String address;
    private Double latitude;
    private Double longitude;
    private String placeId;

    public LocationDTO() {
    }
}
