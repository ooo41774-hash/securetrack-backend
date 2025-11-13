package com.sprboot.sprboot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.sprboot.sprboot.entity.Unit;

public interface UnitRepository extends JpaRepository<Unit, Long> {

        // validate unit existence of a user
        @Query("""
                        SELECT u FROM Unit u
                        JOIN u.batch b
                        JOIN b.product p
                        JOIN p.registrar us
                        WHERE u.unitID IN :ids AND us.userID = :senderID
                        """)
        List<Unit> findAllByIdAndSender(@Param("ids") List<Long> ids, @Param("senderID") Long senderID);

        @Modifying
        @Query("UPDATE Unit u SET u.qrCodeURL = :qrCodeURL WHERE u.unitID = :unitID")
        int updateQrCodeUrl(@Param("qrCodeURL") String qrCodeUrl, @Param("unitID") Long unitID);

        @Query("""
                        SELECT u FROM Unit u
                        WHERE u.serialNumber = :serialNumber
                        AND u.currentCustodianID = :userID
                         """)
        Optional<Unit> findBySerialNumber(String serialNumber, Long userID);

        @Query("""
                        SELECT u.serialNumber FROM Unit u
                        JOIN u.batch b
                        JOIN b.product p
                        WHERE p.registrar.userID = :userID
                        AND u.serialNumber IN :serialNumbers
                        """)
        List<String> checkIfSerialNumberExist(@Param("serialNumbers") List<String> serialNumbers,
                        @Param("userID") Long userID);

        @Query("""
                        SELECT u FROM Unit u
                        JOIN FETCH u.batch b
                        JOIN FETCH b.product p
                        WHERE p.registrar.userID = :userID
                                """)
        List<Unit> findByRegistrar(@Param("userID") Long userID);

        @Query("""
                        SELECT u FROM Unit u
                        JOIN FETCH u.batch b
                        JOIN FETCH b.product p
                        WHERE u.currentCustodianID = :currentCustodianID
                        ORDER BY p.createdDate DESC
                        """)
        List<Unit> getCustodySummary(@Param("currentCustodianID") Long currentCustodianID);

        @Query("""
                        SELECT u FROM Unit u
                        JOIN FETCH u.batch b
                        JOIN FETCH b.product p
                        WHERE u.currentCustodianID = :currentCustodianID
                        AND u.currentCustodianID <> p.registrar.userID
                        """)
        List<Unit> findByCustodian(@Param("currentCustodianID") Long currentCustodianID);

        @Transactional
        @Modifying
        @Query("""
                        UPDATE Unit u SET u.currentCustodianID = :currentCustodianID, u.status = 'received'
                                WHERE u.unitID IN (
                                        SELECT th.unit.unitID FROM TraceabilityHistory th WHERE th.shipment.shipmentID = :shipmentID
                                )
                                """)
        int updateUnitCustodianAndStatusReceived(@Param("shipmentID") Long shipmentID,
                        @Param("currentCustodianID") Long currentCustodianID);

        @Query("""
                        SELECT u.serialNumber
                        FROM Unit u
                        WHERE u.unitID IN :unitIDs
                        AND u.currentCustodianID <> :senderID
                        """)
        List<String> findUnitsNotOwnedBySender(@Param("unitIDs") List<Long> unitIDs,
                        @Param("senderID") Long senderID);

        @Transactional
        @Modifying
        @Query("""
                        UPDATE Unit u SET u.status = 'pending'
                        WHERE u.unitID IN (
                                SELECT th.unit.unitID FROM TraceabilityHistory th WHERE th.shipment.shipmentID = :shipmentID
                        )
                        """)
        int updateUnitStatusPending(@Param("shipmentID") Long shipmentID);

        @Transactional
        @Modifying
        @Query("""
                        UPDATE Unit u SET u.status = 'shipped'
                        WHERE u.unitID IN (
                                SELECT th.unit.unitID FROM TraceabilityHistory th WHERE th.shipment.shipmentID = :shipmentID
                        )
                        """)
        int updateUnitStatusShipped(@Param("shipmentID") Long shipmentID);

        @Transactional
        @Modifying
        @Query("""
                        UPDATE Unit u SET u.status = 'received'
                        WHERE u.unitID IN :unitIDs
                        """)
        int updateUnitStatusReceived(@Param("unitIDs") List<Long> unitIDs);

        @Transactional
        @Modifying
        @Query("""
                        UPDATE Unit u SET u.status = 'received'
                        WHERE u.unitID IN :unitIDs
                        """)
        int updateUnitStatusCreated(@Param("unitIDs") List<Long> unitIDs);
}
