package com.sprboot.sprboot.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sprboot.sprboot.entity.Batch;

public interface BatchRepository extends JpaRepository<Batch, Long> {
        @Query("""
                        SELECT DISTINCT b
                        FROM Batch b
                        JOIN FETCH b.units u
                        WHERE b IN :batches
                        """)
        List<Batch> findBatchWithUnits(@Param("batches") List<Batch> batches);

        @Query("""
                        SELECT b FROM Batch b
                        WHERE b.product.registrar.userID = :userID
                        AND b.product.productCode IN :productCodes
                        AND b.batchNumber IN :batchNumbers
                        """)
        List<Batch> findAllByProductCodesAndBatchNumber(
                        @Param("userID") Long userID,
                        @Param("productCodes") Set<String> productCodes,
                        @Param("batchNumbers") Set<String> batchNumbers);
}
