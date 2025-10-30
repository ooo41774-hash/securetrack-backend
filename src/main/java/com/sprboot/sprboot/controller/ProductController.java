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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sprboot.sprboot.dto.CustodyInfoDTO;
import com.sprboot.sprboot.dto.ProductDTO;
import com.sprboot.sprboot.dto.ProductRequest;
import com.sprboot.sprboot.dto.ProductInfoDTO;
import com.sprboot.sprboot.dto.VerifyProductResponse;
import com.sprboot.sprboot.service.ProductService;

@RestController
@RequestMapping("/api/product/")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/dashboard/my-registered-product")
    public ResponseEntity<List<ProductInfoDTO>> getMyRegisteredProductSummary(@RequestParam Long userID) {
        return ResponseEntity.ok(productService.getMyRegisteredProductSummary(userID));
    }

    @GetMapping("/dashboard/my-custody")
    public ResponseEntity<List<CustodyInfoDTO>> getMyCustodySummary(@RequestParam Long userID) {
        return ResponseEntity.ok(productService.getMyCustodySummary(userID));
    }

    // @GetMapping("/my-registered")
    // public ResponseEntity<List<ProductDTO>> getMyRegistered(@RequestParam Long
    // userID) {
    // return ResponseEntity.ok(productService.getMyRegistered(userID));
    // }

    @GetMapping("/my-custody/selfRegistered")
    public ResponseEntity<List<CustodyInfoDTO>> getMyCustodySelfRegistered(@RequestParam Long userID) {
        return ResponseEntity.ok(productService.getMyCustodySelfRegistered(userID));
    }

    @GetMapping("/my-custody/received")
    public ResponseEntity<List<CustodyInfoDTO>> getMyCustodyReceived(@RequestParam Long userID) {
        return ResponseEntity.ok(productService.getMyCustodyReceived(userID));
    }

    @GetMapping("/details")
    public ResponseEntity<ProductDTO> getProductDetails(@RequestParam Long productID) {
        return ResponseEntity.ok(productService.getProductDetails(productID));
    }

    @PostMapping(value = "/addProduct", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> createProduct(@ModelAttribute ProductRequest productRequest) {

        MultipartFile file = productRequest.getFile();
        String filename = file.getOriginalFilename();
        if (filename == null ||
                !(filename.endsWith(".csv") || filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            throw new IllegalArgumentException("Only CSV or Excel files are allowed.");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        filename = "product_qrcodes_" + timestamp + ".pdf";
        productRequest.setFilename(filename);

        byte[] pdfBytes = productService.createProduct(productRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/verifyProduct")
    public ResponseEntity<VerifyProductResponse> verifyProduct(@RequestParam String param) {
        return new ResponseEntity<VerifyProductResponse>(productService.verifyProduct(param), HttpStatus.CREATED);
    }

}
