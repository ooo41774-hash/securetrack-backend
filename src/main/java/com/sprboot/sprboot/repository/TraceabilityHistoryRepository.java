package com.sprboot.sprboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sprboot.sprboot.entity.Product;
import com.sprboot.sprboot.entity.Unit;
import com.sprboot.sprboot.entity.Shipment;
import com.sprboot.sprboot.entity.TraceabilityHistory;

public interface TraceabilityHistoryRepository extends JpaRepository<TraceabilityHistory, Long> {

        @Query("""
                        SELECT th.unit
                        FROM TraceabilityHistory th
                        WHERE th.shipment.shipmentID = :shipmentID
                        """)
        List<Unit> getUnitByShipmentID(@Param("shipmentID") Long shipmentID);

        @Modifying
        @Query("UPDATE TraceabilityHistory th SET th.status = \"completed\" WHERE th.shipment.shipmentID = :shipmentID")
        int updateTraceabilityHistoryStatus(@Param("shipmentID") Long shipmentID);

        @Query("""
                        SELECT DISTINCT p
                        FROM Product p
                        JOIN FETCH p.batches b
                        WHERE EXISTS (
                            SELECT th
                            FROM TraceabilityHistory th
                            WHERE th.unit.batch = b
                            AND th.shipment.shipmentID = :shipmentID
                        )
                        """)
        List<Product> findAllProductByShipment(@Param("shipmentID") Long shipmentID);

        @Query("""
                        SELECT th.shipment
                        FROM TraceabilityHistory th
                        JOIN FETCH th.shipment.sender
                        JOIN FETCH th.shipment.receiver
                        WHERE th.unit.unitID = :unitID
                        AND th.shipment.status = 'received'
                        AND th.status = 'completed'
                        ORDER BY th.shipment.receivedTimestamp
                        """)
        List<Shipment> findTraceabilityHistory(@Param("unitID") Long unitID);

        boolean existsByUnit_UnitIDAndShipment_StatusNot(long unitID, String string);
}
