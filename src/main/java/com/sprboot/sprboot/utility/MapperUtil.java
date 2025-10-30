package com.sprboot.sprboot.utility;

import java.util.ArrayList;
import java.util.List;

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
}
