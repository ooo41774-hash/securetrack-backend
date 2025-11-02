package com.sprboot.sprboot.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprboot.sprboot.dto.AddShipmentRequest;
import com.sprboot.sprboot.dto.ShipmentDTO;
import com.sprboot.sprboot.dto.ShipmentRequest;
import com.sprboot.sprboot.dto.VerifyShipmentResponse;
import com.sprboot.sprboot.service.ShipmentService;

@RestController
@RequestMapping("/api/shipment/")
public class ShipmentController {

    private ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<ShipmentDTO>> getIncomingShipment(@RequestParam Long userID) {
        return ResponseEntity.ok(shipmentService.getIncomingShipment(userID));
    }

    @GetMapping("/outgoing")
    public ResponseEntity<List<ShipmentDTO>> getOutgoingShipment(@RequestParam Long userID) {
        return ResponseEntity.ok(shipmentService.getOutgoingShipment(userID));
    }

    @GetMapping("/details")
    public ResponseEntity<ShipmentDTO> getShipmentDetails(@RequestParam Long shipmentID) {
        return ResponseEntity.ok(shipmentService.getShipmentDetails(shipmentID));
    }

    @PostMapping(value = "/addShipment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> createShipment(@ModelAttribute AddShipmentRequest addShipmentRequest) {

        MultipartFile file = addShipmentRequest.getFile();
        if (file != null) {
            String inputFilename = file.getOriginalFilename();
            if (inputFilename == null ||
                    !(inputFilename.endsWith(".csv") || inputFilename.endsWith(".xlsx")
                            || inputFilename.endsWith(".xls"))) {
                throw new IllegalArgumentException("Only CSV or Excel files are allowed.");
            }
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String outputFilename = "shipment_qrcodes_" + timestamp + ".pdf";
        addShipmentRequest.setFilename(outputFilename);

        byte[] pdfBytes = shipmentService.createShipment(addShipmentRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputFilename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/verifyShipment")
    public ResponseEntity<VerifyShipmentResponse> verifyShipment(
            @RequestParam Long userID, @RequestParam String cipherText) {
        return ResponseEntity.ok(shipmentService.verifyShipment(userID, cipherText));
    }

    @PostMapping("/sendShipment")
    public ResponseEntity<Boolean> sendShipment(@RequestBody ShipmentRequest shipmentRequest) {
        return new ResponseEntity<Boolean>(shipmentService.sendShipment(shipmentRequest),
                HttpStatus.ACCEPTED);
    }

    @PostMapping("/receiveShipment")
    public ResponseEntity<Boolean> receiveShipment(@RequestBody ShipmentRequest shipmentRequest) {
        return new ResponseEntity<Boolean>(shipmentService.receiveShipment(shipmentRequest),
                HttpStatus.ACCEPTED);
    }

    @PostMapping("/recallShipment")
    public ResponseEntity<Boolean> recallShipment(@RequestBody ShipmentRequest shipmentRequest) {
        return new ResponseEntity<Boolean>(shipmentService.recallShipment(shipmentRequest),
                HttpStatus.ACCEPTED);
    }

}
