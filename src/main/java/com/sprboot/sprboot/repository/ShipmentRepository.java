package com.sprboot.sprboot.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sprboot.sprboot.entity.Shipment;
import com.sprboot.sprboot.entity.User;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

        @Query("SELECT s.sender FROM Shipment s WHERE s.sender.userID = :userID")
        User findSenderByUserID(@Param("userID") Long userID);

        @Query("SELECT s.receiver FROM Shipment s WHERE s.receiver.userID = :userID")
        User findReceiverByUserID(@Param("userID") Long userID);

        @Modifying
        @Query("UPDATE Shipment s SET s.qrCodeUrl = :qrCodeUrl WHERE s.shipmentID = :shipmentID")
        int updateQrCodeUrl(@Param("qrCodeUrl") String qrCodeUrl, @Param("shipmentID") Long shipmentID);

        @Modifying
        @Query("UPDATE Shipment s SET s.status = 'inTransit', s.sentTimestamp = :sentTimestamp WHERE s.shipmentID = :shipmentID AND s.status <> 'recalled'")
        int updateShipmentInTransit(@Param("sentTimestamp") LocalDateTime sentTimestamp,
                        @Param("shipmentID") Long shipmentID);

        @Modifying
        @Query("UPDATE Shipment s SET s.status = 'received', s.receivedTimestamp = :receivedTimestamp WHERE s.shipmentID = :shipmentID AND s.status <> 'recalled'")
        int updateShipmentReceived(@Param("receivedTimestamp") LocalDateTime receivedTimestamp,
                        @Param("shipmentID") Long shipmentID);

        @Modifying
        @Query("UPDATE Shipment s SET s.status = 'recalled', s.recalledTimestamp = :recalledTimestamp WHERE s.shipmentID = :shipmentID")
        int updateShipmentRecalled(@Param("recalledTimestamp") LocalDateTime recalledTimestamp,
                        @Param("shipmentID") Long shipmentID);

        @Query("""
                                 SELECT s FROM Shipment s
                                 JOIN FETCH s.sender
                                 JOIN FETCH s.receiver
                                 WHERE s.sender.userID = :userID AND s.status IN ('pending', 'inTransit', 'received', 'recalled')
                        """)
        List<Shipment> getOutgoingShipment(@Param("userID") Long userID);

        @Query("""
                                 SELECT s FROM Shipment s
                                 JOIN FETCH s.sender
                                 JOIN FETCH s.receiver
                                  WHERE s.receiver.userID = :userID AND s.status IN ('pending', 'inTransit', 'received', 'recalled')
                        """)
        List<Shipment> getIncomingShipment(@Param("userID") Long userID);

        @Query("""
                                 SELECT s FROM Shipment s
                                 JOIN FETCH s.receiver us
                                 JOIN FETCH s.traceabilityHistory th
                                 JOIN FETCH th.unit u
                                 WHERE u.unitID = :unitID
                                 AND s.sender.userID = :userID
                                 AND s.status <> 'recalled'
                        """)
        Shipment findShipmentReceiverByUnit(@Param("unitID") Long unitID, @Param("userID") Long userID);

        @Query("""
                                 SELECT s FROM Shipment s
                                 JOIN FETCH s.sender us
                                 JOIN FETCH s.traceabilityHistory th
                                 JOIN FETCH th.unit u
                                 WHERE u.unitID = :unitID
                                 AND s.receiver.userID = :userID
                                 AND s.status <> 'recalled'
                        """)
        Shipment findShipmentSenderByUnit(@Param("unitID") Long unitID, @Param("userID") Long userID);

        @Query("""
                                 SELECT s FROM Shipment s
                                 JOIN FETCH s.receiver
                                 WHERE s.shipmentID = :shipmentID
                        """)
        Shipment getShipmentDetails(@Param("shipmentID") Long shipmentID);

}
