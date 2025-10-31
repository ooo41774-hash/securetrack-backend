package com.sprboot.sprboot.utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sprboot.sprboot.dto.BatchDTO;
import com.sprboot.sprboot.dto.ProductDTO;
import com.sprboot.sprboot.dto.UnitDTO;
import com.sprboot.sprboot.entity.Batch;
import com.sprboot.sprboot.entity.Product;
import com.sprboot.sprboot.entity.Unit;

public class MapperUtil {

    public static ProductDTO toHierarchy(Product product) {

        ProductDTO productDTO = new ProductDTO(
                product.getProductCode(),
                product.getProductName());

        List<BatchDTO> batchDTOs = new ArrayList<>();

        for (Batch b : product.getBatches()) {
            BatchDTO batchDTO = new BatchDTO(
                    b.getBatchNumber());

            List<UnitDTO> unitDTOs = new ArrayList<>();

            for (Unit u : b.getUnits()) {
                UnitDTO unitDTO = new UnitDTO(
                        u.getSerialNumber(),
                        u.getDescription());
                unitDTO.setCreatedTimestamp(u.getCreatedTimestamp());
                unitDTO.setStatus(u.getStatus());
                unitDTOs.add(unitDTO);
            }

            batchDTO.setUnits(unitDTOs);
            batchDTOs.add(batchDTO);
        }

        productDTO.setBatches(batchDTOs);

        return productDTO;
    }

    public static List<ProductDTO> toHierarchy(List<Product> products) {
        List<ProductDTO> productDTOs = new ArrayList<>();

        for (Product p : products) {
            ProductDTO productDTO = new ProductDTO(
                    p.getProductCode(),
                    p.getProductName());

            List<BatchDTO> batchDTOs = new ArrayList<>();

            for (Batch b : p.getBatches()) {
                BatchDTO batchDTO = new BatchDTO(
                        b.getBatchNumber());

                List<UnitDTO> unitDTOs = new ArrayList<>();

                for (Unit u : b.getUnits()) {
                    UnitDTO unitDTO = new UnitDTO(
                            u.getSerialNumber(),
                            u.getDescription());
                    unitDTO.setCreatedTimestamp(u.getCreatedTimestamp());
                    unitDTO.setStatus(u.getStatus());
                    unitDTOs.add(unitDTO);
                }

                batchDTO.setUnits(unitDTOs);
                batchDTOs.add(batchDTO);
            }

            productDTO.setBatches(batchDTOs);

            productDTOs.add(productDTO);
        }
        return productDTOs;
    }

    public static List<ProductDTO> toHierarchyDTO(List<Unit> units) {

        // Group by product
        Map<Product, List<Unit>> productMap = units.stream()
                .collect(Collectors.groupingBy(u -> u.getBatch().getProduct()));

        // For each product, group its batches
        return productMap.entrySet().stream()
                .map(productEntry -> {
                    Product product = productEntry.getKey();
                    List<Unit> productUnits = productEntry.getValue();

                    // Group by batch number within each product
                    Map<String, List<Unit>> batchMap = productUnits.stream()
                            .collect(Collectors.groupingBy(u -> u.getBatch().getBatchNumber()));

                    // Build batch DTOs
                    List<BatchDTO> batchDTOs = batchMap.entrySet().stream()
                            .map(batchEntry -> {
                                String batchNumber = batchEntry.getKey();
                                List<UnitDTO> unitDTOs = batchEntry.getValue().stream()
                                        .map(u -> new UnitDTO(u.getSerialNumber()))
                                        .collect(Collectors.toList());

                                BatchDTO batchDTO = new BatchDTO();
                                batchDTO.setBatchNumber(batchNumber);
                                batchDTO.setUnits(unitDTOs);
                                return batchDTO;
                            })
                            .collect(Collectors.toList());

                    // Build product DTO
                    ProductDTO productDTO = new ProductDTO();
                    productDTO.setProductCode(product.getProductCode());
                    productDTO.setProductName(product.getProductName());
                    productDTO.setBatches(batchDTOs);

                    return productDTO;
                })
                .collect(Collectors.toList());
    }
}
