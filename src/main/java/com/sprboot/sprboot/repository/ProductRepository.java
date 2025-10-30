package com.sprboot.sprboot.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sprboot.sprboot.dto.ProductInfoDTO;
import com.sprboot.sprboot.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

        @Query("SELECT p FROM Product p WHERE p.registrar.userID = :userID AND p.productCode IN :productCodes")
        List<Product> findAllByProductCodes(@Param("userID") Long userID,
                        @Param("productCodes") Set<String> productCodes);

        @Query("""
                        SELECT DISTINCT p FROM Product p
                        JOIN FETCH p.registrar r
                        LEFT JOIN FETCH p.batches b
                        LEFT JOIN FETCH b.units u
                        WHERE r.userID = :registrarID
                        """)
        List<Product> findDistinctByRegistrar(@Param("registrarID") Long registrarID);

        @Query("""
                        SELECT DISTINCT p FROM Product p
                        LEFT JOIN FETCH p.batches b
                        LEFT JOIN FETCH b.units u
                        WHERE u.currentCustodianID = :currentCustodianID
                        """)
        List<Product> findDistinctByCurrentCustodianID(@Param("currentCustodianID") Long currentCustodianID);

        @Query("""
                        SELECT new com.sprboot.sprboot.dto.ProductInfoDTO(
                                p.productID, p.productCode, p.productName, COUNT(DISTINCT b.batchID), COUNT(u.unitID), p.createdDate
                        )
                                FROM Product p
                                LEFT JOIN p.batches b
                                LEFT JOIN b.units u
                                WHERE p.registrar.userID = :userID
                                GROUP BY p.productID, p.productCode, p.productName, p.createdDate
                        """)
        List<ProductInfoDTO> getProductSummary(@Param("userID") Long userID);

        @Query("""
                        SELECT p FROM Product p
                        LEFT JOIN p.batches b
                        LEFT JOIN b.units u
                        WHERE p.productID = :productID
                        """)
        Product getProductDetails(@Param("productID") Long productID);

}
