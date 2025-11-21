package com.sprboot.sprboot.service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sprboot.sprboot.dto.AddShipmentRequest;
import com.sprboot.sprboot.dto.BatchDTO;
import com.sprboot.sprboot.dto.CustodyInfoDTO;
import com.sprboot.sprboot.dto.ProductDTO;
import com.sprboot.sprboot.dto.UnitDTO;
import com.sprboot.sprboot.dto.ShipmentDTO;
import com.sprboot.sprboot.dto.ShipmentRequest;
import com.sprboot.sprboot.dto.VerifyShipmentResponse;
import com.sprboot.sprboot.entity.Location;
import com.sprboot.sprboot.entity.Product;
import com.sprboot.sprboot.entity.Unit;
import com.sprboot.sprboot.entity.Shipment;
import com.sprboot.sprboot.entity.TraceabilityHistory;
import com.sprboot.sprboot.entity.User;
import com.sprboot.sprboot.repository.UnitRepository;
import com.sprboot.sprboot.repository.LocationRepository;
import com.sprboot.sprboot.repository.ShipmentRepository;
import com.sprboot.sprboot.repository.TraceabilityHistoryRepository;
import com.sprboot.sprboot.repository.UserRepository;
import com.sprboot.sprboot.utility.AESUtil;
import com.sprboot.sprboot.utility.HmacUtil;
import com.sprboot.sprboot.utility.MapperUtil;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class ShipmentService {

        @Value("${frontend.shipment.path}")
        private String path;

        @Value("${sec.secretkeyhmac}")
        private String secretKeyHmac;

        @Value("${sec.secretkeyaes}")
        private String secretKeyAes;

        @Value("${resources.saveDir.shipmentqr}")
        private String saveDirShipmentQR;

        @Value("${app.thresholdMeters}")
        private double thresholdMeters;

        private ShipmentRepository shipmentRepository;
        private UserRepository userRepository;
        private UnitRepository unitRepository;
        private TraceabilityHistoryRepository traceabilityHistoryRepository;
        private LocationRepository locationRepository;

        public ShipmentService(ShipmentRepository shipmentRepository,
                        UserRepository userRepository,
                        UnitRepository unitRepository,
                        TraceabilityHistoryRepository traceabilityHistoryRepository,
                        LocationRepository locationRepository) {
                this.shipmentRepository = shipmentRepository;
                this.userRepository = userRepository;
                this.unitRepository = unitRepository;
                this.traceabilityHistoryRepository = traceabilityHistoryRepository;
                this.locationRepository = locationRepository;
        }

        @Transactional
        public byte[] createShipment(AddShipmentRequest addShipmentRequest) {

                MultipartFile file = addShipmentRequest.getFile();
                List<String> serialNumbers = new ArrayList<>();
                if (file != null) {
                        serialNumbers = parseSerialNumbers(file);
                } else {
                        serialNumbers = addShipmentRequest.getSerialNumbers();
                }

                SecretKey key = AESUtil.getKeyFromString(secretKeyAes);

                // validating sender and receiver
                User sender = userRepository.findById(addShipmentRequest.getSenderID())
                                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

                User receiver = userRepository.findById(addShipmentRequest.getReceiverID())
                                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

                List<Unit> units = new ArrayList<>();
                List<String> invalidSerials = new ArrayList<>();
                for (String serialNumber : serialNumbers) {
                        Unit u = unitRepository.findBySerialNumber(serialNumber, sender.getUserID())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "Product unit not found, serial number : " + serialNumber));

                        // Check in-transit units
                        if (u.getStatus().equals("pending") || u.getStatus().equals("shipped")) {
                                invalidSerials.add(u.getSerialNumber());
                        } else
                                units.add(u);
                }

                // Block in transit serial numbers
                if (invalidSerials.size() > 0) {
                        throw new IllegalArgumentException("Units still in transit: " + invalidSerials);
                }

                List<Long> unitIDs = units.stream().map(u -> u.getUnitID()).toList();

                // check if the sender is current custodian
                System.out.println("Sender ID: " + sender.getUserID());
                List<String> invalidUnits = unitRepository.findUnitsNotOwnedBySender(unitIDs, sender.getUserID());
                if (!invalidUnits.isEmpty()) {
                        throw new IllegalArgumentException("Units not under custody: " + invalidUnits);
                }

                // generate HMAC Hash
                String senderID = String.valueOf(addShipmentRequest.getSenderID());
                String receiverID = String.valueOf(addShipmentRequest.getReceiverID());
                String stringUnitIDs = units.stream().map(u -> String.valueOf(u.getUnitID()))
                                .collect(Collectors.joining(","));

                System.out.println("Joined unitIDs: " + stringUnitIDs);

                String data = senderID + receiverID + stringUnitIDs;

                // generate HMAC Hash
                String hmacHash = HmacUtil.generateHmac(data, secretKeyHmac);

                // save location
                Location location = new Location();
                location.setAddress(addShipmentRequest.getAddress());
                location.setLatitude(addShipmentRequest.getLatitude());
                location.setLongitude(addShipmentRequest.getLongitude());
                location.setPlaceId(addShipmentRequest.getPlaceId());
                Location savedLocation = locationRepository.save(location);

                // save shipment
                Shipment shipment = new Shipment();
                shipment.setQrCodeUrl("");
                shipment.setHMACHash(hmacHash);
                shipment.setCreationTimestamp(LocalDateTime.now());
                shipment.setSender(sender);
                shipment.setReceiver(receiver);
                shipment.setDestination(savedLocation);
                Shipment savedShipment = shipmentRepository.save(shipment);

                // encrypt shipment id and hmac hash
                Long shipmentID = savedShipment.getShipmentID();
                String input = String.valueOf(shipmentID) + "|" + hmacHash;
                String encrypted = null;
                try {
                        encrypted = AESUtil.encrypt(input, key);
                        System.out.println("Encrypted = " + encrypted);
                } catch (Exception e) {
                        System.out.println("[Shipment Service] Exception = " + e.getMessage());

                }

                // constructing QR Code URL
                String url = null;
                if (encrypted != null) {
                        url = path + encrypted;
                }

                // save url to db
                int result = shipmentRepository.updateQrCodeUrl(url, shipmentID);
                savedShipment.setQrCodeUrl(url);
                if (result < 1) {
                        throw new IllegalArgumentException("Url of shipment " + shipmentID + " cannot be updated");
                }

                // save Traceability history
                // establish relationship between shipment and product unit
                for (Unit u : units) {
                        TraceabilityHistory shipment_unit = new TraceabilityHistory();
                        shipment_unit.setShipment(shipment);
                        shipment_unit.setUnit(u);
                        traceabilityHistoryRepository.save(shipment_unit);
                }

                // update unit status
                unitRepository.updateUnitStatusPending(savedShipment.getShipmentID());

                String filename = addShipmentRequest.getFilename();

                byte[] pdfBytes = null;
                try {
                        pdfBytes = PdfGeneratorService.generateQrCodePdf(shipment, saveDirShipmentQR, filename);
                } catch (Exception e) {
                        e.printStackTrace();
                }

                return pdfBytes;
        }

        @Transactional
        public VerifyShipmentResponse verifyShipment(Long userID, String cipherText) {

                String action = "blockAccess";

                SecretKey key = AESUtil.getKeyFromString(secretKeyAes);

                String decrypt = null;
                Long shipmentID = null;
                String hashFromQR = null;

                // decrypt cipher Text
                try {
                        decrypt = AESUtil.decrypt(cipherText, key);
                } catch (Exception e) {
                        System.out.println("[Shipment Service] Exception = " + e.getMessage());
                }

                // extract shipmentID and hash
                if (decrypt != null) {
                        String[] parts = decrypt.split("\\|");
                        shipmentID = Long.valueOf(parts[0]);
                        hashFromQR = parts[1];
                }

                // find the shipment record with shipmentID
                Shipment shipment = shipmentRepository.findById(shipmentID)
                                .orElseThrow(() -> new IllegalArgumentException("Shipment ID not found"));

                // verify if hmac hash match
                if (!shipment.getHMACHash().equals(hashFromQR)) {
                        throw new IllegalArgumentException("Hash value does not match");
                }

                User sender = shipment.getSender();
                User receiver = shipment.getReceiver();

                if (userID != sender.getUserID() && userID != receiver.getUserID()) {
                        throw new IllegalArgumentException("Access Blocked");
                }

                // determine user action
                if (sender.getUserID() == userID) {
                        if (shipment.getStatus().equals("pending")) {
                                action = "sendShipment";
                        } else if (shipment.getStatus().equals("inTransit")
                                        || shipment.getStatus().equals("received")) {
                                action = "viewShipment";
                        }
                } else if (receiver.getUserID() == userID) {
                        if (shipment.getStatus().equals("inTransit")) {
                                action = "receiveShipment";
                        } else if (shipment.getStatus().equals("received") || shipment.getStatus().equals("pending")) {
                                action = "viewShipment";
                        }
                }

                // get shipment unit
                List<Unit> shipmentUnits = traceabilityHistoryRepository.getUnitByShipmentID(shipmentID);
                List<ProductDTO> productDTOs = MapperUtil.toHierarchyDTO(shipmentUnits);

                return new VerifyShipmentResponse(shipment.getShipmentID(), sender.getUsername(),
                                receiver.getUsername(),
                                shipment.getDestination().getAddress(), productDTOs, action, shipment.getStatus());
        }

        @Transactional
        public boolean sendShipment(ShipmentRequest shipmentRequest) {
                Long shipmentID = shipmentRequest.getShipmentID();

                try {
                        Shipment shipment = shipmentRepository.findById(shipmentID)
                                        .orElseThrow(() -> new IllegalArgumentException("Shipment ID not found"));

                        // check if the user is sender
                        if (shipment.getSender().getUserID() != shipmentRequest.getUserID()) {
                                throw new IllegalArgumentException("User not sender!");
                        }

                        // check if the shipment status = pending
                        if (!shipment.getStatus().equals("pending")) {
                                throw new IllegalArgumentException("Shipment already sent!");
                        }

                        // update shipment status = inTransit
                        shipmentRepository.updateShipmentInTransit(LocalDateTime.now(), shipmentID);

                        // update unit status = shipped
                        unitRepository.updateUnitStatusShipped(shipmentID);

                        return true;
                } catch (Exception e) {
                        throw new IllegalArgumentException("Sending Shipment Error: " + e.getMessage());
                }
        }

        @Transactional
        public boolean receiveShipment(ShipmentRequest shipmentRequest) {
                Long shipmentID = shipmentRequest.getShipmentID();

                try {
                        Shipment shipment = shipmentRepository.findById(shipmentID)
                                        .orElseThrow(() -> new IllegalArgumentException("Shipment ID not found"));

                        // check if the user is receiver
                        if (shipment.getReceiver().getUserID() != shipmentRequest.getUserID()) {
                                throw new IllegalArgumentException("User not receiver!");
                        }

                        // check if the shipment status = inTransit
                        if (!shipment.getStatus().equals("inTransit")) {
                                throw new IllegalArgumentException("Shipment not in transit");
                        }

                        // check if the receiver current location matches intended shipment location
                        Location intendedLocation = shipment.getDestination();
                        double intendedLat = intendedLocation.getLatitude();
                        double intendedLon = intendedLocation.getLongitude();
                        double currentLat = shipmentRequest.getLatitude();
                        double currentLon = shipmentRequest.getLongitude();

                        double distance = haversineDistance(intendedLat, intendedLon, currentLat, currentLon);
                        boolean match = distance <= thresholdMeters;

                        if (!match) {
                                throw new IllegalArgumentException(
                                                "Location mismatch. \nPlease turn on your phone location (GPS) for accurate verification.");
                        }

                        // update Shipment status received
                        shipmentRepository.updateShipmentReceived(LocalDateTime.now(), shipmentID);

                        // update unit History record
                        traceabilityHistoryRepository.updateTraceabilityHistoryStatus(shipmentID);

                        // update current custodian = receiver and status = received
                        unitRepository.updateUnitCustodianAndStatusReceived(shipmentID,
                                        shipment.getReceiver().getUserID());

                        return true;
                } catch (Exception e) {
                        throw new IllegalArgumentException(e.getMessage());

                }
        }

        @Transactional
        public Boolean recallShipment(ShipmentRequest shipmentRequest) {
                try {
                        long shipmentID = shipmentRequest.getShipmentID();
                        long userID = shipmentRequest.getUserID();

                        Shipment shipment = shipmentRepository.findById(shipmentID)
                                        .orElseThrow(() -> new IllegalArgumentException("Shipment ID not found"));

                        // check if shipment received
                        if (shipment.getStatus().equals("received")) {
                                throw new IllegalArgumentException("Shipment already received!");
                        }

                        // check if shipment recalled
                        if (shipment.getStatus().equals("recalled")) {
                                throw new IllegalArgumentException("Shipment already recalled!");
                        }

                        // update shipment status = recalled
                        shipmentRepository.updateShipmentRecalled(LocalDateTime.now(), shipmentRequest.getShipmentID());

                        // update current custodian = sender and status = created OR received
                        List<Unit> shipmentUnits = traceabilityHistoryRepository.getUnitByShipmentID(shipmentID);
                        List<Long> selfRegisteredUnits = new ArrayList<>();
                        List<Long> receivedUnits = new ArrayList<>();
                        for (Unit u : shipmentUnits) {
                                if (u.getCurrentCustodianID() == u.getBatch().getProduct().getRegistrar().getUserID()) {
                                        selfRegisteredUnits.add(u.getUnitID());
                                } else {
                                        receivedUnits.add(u.getUnitID());
                                }
                        }
                        unitRepository.updateUnitStatusCreated(selfRegisteredUnits);
                        unitRepository.updateUnitStatusReceived(receivedUnits);

                        return true;
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new IllegalArgumentException("Recall Shipment Error: " + e.getMessage());
                }
        }

        private List<String> parseSerialNumbers(MultipartFile file) {

                List<String> serialNumbers = new ArrayList<>();

                String originalFilename = file.getOriginalFilename();
                boolean isExcel = originalFilename != null &&
                                (originalFilename.endsWith(".xlsx") || originalFilename.endsWith(".xls"));

                try {
                        if (isExcel) {
                                try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
                                        Sheet sheet = workbook.getSheetAt(0);
                                        Iterator<Row> rowIterator = sheet.iterator();

                                        if (!rowIterator.hasNext()) {
                                                throw new IllegalArgumentException("Excel file is empty.");
                                        }

                                        // Read header
                                        Row headerRow = rowIterator.next();
                                        int serialNumberColIndex = -1;

                                        for (Cell cell : headerRow) {
                                                if (cell.getStringCellValue().trim()
                                                                .equalsIgnoreCase("serial_number")) {
                                                        serialNumberColIndex = cell.getColumnIndex();
                                                        break;
                                                }
                                        }

                                        if (serialNumberColIndex == -1) {
                                                throw new IllegalArgumentException(
                                                                "serial_number column not found in Excel file.");
                                        }

                                        int rowIndex = 1; // For user-friendly error message (starts after header)
                                        while (rowIterator.hasNext()) {
                                                rowIndex++;
                                                Row row = rowIterator.next();
                                                Cell cell = row.getCell(serialNumberColIndex);

                                                if (cell == null || cell.toString().trim().isEmpty()) {
                                                        throw new IllegalArgumentException(
                                                                        "Empty 'serial_number' found at row "
                                                                                        + rowIndex);
                                                }

                                                String serial = cell.toString().trim();
                                                serialNumbers.add(serial);
                                        }
                                }
                        } else {
                                // CSV parsing
                                try (Reader reader = new InputStreamReader(file.getInputStream())) {
                                        CSVParser parser = new CSVParser(reader,
                                                        CSVFormat.DEFAULT.withFirstRecordAsHeader());
                                        List<CSVRecord> records = parser.getRecords();

                                        if (records.isEmpty()) {
                                                throw new IllegalArgumentException("CSV file is empty.");
                                        }

                                        int rowIndex = 1;
                                        for (CSVRecord record : records) {
                                                rowIndex++;
                                                String serialNumber = record.get("serial_number");

                                                if (serialNumber == null || serialNumber.trim().isEmpty()) {
                                                        throw new IllegalArgumentException(
                                                                        "Empty 'serial_number' found at line "
                                                                                        + rowIndex);
                                                }

                                                serialNumbers.add(serialNumber.trim());
                                        }
                                }
                        }
                } catch (Exception e) {
                        throw new IllegalArgumentException("Error reading file: " + e.getMessage());
                }

                return serialNumbers;
        }

        public List<ShipmentDTO> getIncomingShipment(Long userID) {
                List<Shipment> shipments = shipmentRepository.getIncomingShipment(userID);
                return shipments.stream()
                                .map(s -> {
                                        ShipmentDTO sDto = new ShipmentDTO();
                                        sDto.setShipmentID(s.getShipmentID());
                                        sDto.setSenderUsername(s.getSender().getUsername());
                                        sDto.setSenderAddress(s.getSender().getLocation().getAddress());
                                        sDto.setStatus(s.getStatus());
                                        sDto.setSentTimestamp(s.getSentTimestamp());
                                        sDto.setReceivedTimestamp(s.getReceivedTimestamp());
                                        sDto.setQrCodeUrl(s.getQrCodeUrl());
                                        return sDto;
                                })
                                .collect(Collectors.toList());
        }

        public List<ShipmentDTO> getOutgoingShipment(Long userID) {
                List<Shipment> shipments = shipmentRepository.getOutgoingShipment(userID);
                return shipments.stream()
                                .map(s -> {
                                        ShipmentDTO sDto = new ShipmentDTO();
                                        sDto.setShipmentID(s.getShipmentID());
                                        sDto.setReceiverUsername(s.getReceiver().getUsername());
                                        sDto.setReceiverAddress(s.getDestination().getAddress());
                                        sDto.setStatus(s.getStatus());
                                        sDto.setSentTimestamp(s.getSentTimestamp());
                                        sDto.setReceivedTimestamp(s.getReceivedTimestamp());
                                        sDto.setQrCodeUrl(s.getQrCodeUrl());
                                        return sDto;
                                })
                                .collect(Collectors.toList());
        }

        private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
                final int R = 6371000; // Earth avg radius 6371km
                double diffLat = Math.toRadians(lat2 - lat1); // find lat diff in rad
                double diffLon = Math.toRadians(lon2 - lon1); // find lon diff in rad
                double a = Math.sin(diffLat / 2) * Math.sin(diffLat / 2) +
                                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                                                Math.sin(diffLon / 2) * Math.sin(diffLon / 2);
                double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                return R * c;
        }

        public ShipmentDTO getShipmentDetails(Long shipmentID) {
                try {
                        Shipment shipment = shipmentRepository.getShipmentDetails(shipmentID);
                        ShipmentDTO sDto = new ShipmentDTO();
                        sDto.setShipmentID(shipmentID);
                        sDto.setReceiverUsername(shipment.getReceiver().getUsername());
                        sDto.setSenderUsername(shipment.getSender().getUsername());
                        sDto.setShipmentAddress(shipment.getDestination().getAddress());
                        sDto.setStatus(shipment.getStatus());
                        sDto.setSentTimestamp(shipment.getSentTimestamp());
                        sDto.setCreationTimestamp(shipment.getCreationTimestamp());
                        sDto.setReceivedTimestamp(shipment.getReceivedTimestamp());

                        List<Unit> units = traceabilityHistoryRepository.getUnitByShipmentID(shipmentID);

                        List<CustodyInfoDTO> custodyInfoDTOs = units.stream()
                                        .map(u -> {
                                                CustodyInfoDTO cDto = new CustodyInfoDTO();
                                                cDto.setSerialNumber(u.getSerialNumber());
                                                cDto.setProductCode(u.getBatch().getProduct().getProductCode());
                                                cDto.setProductName(u.getBatch().getProduct().getProductName());
                                                cDto.setBatchNumber(u.getBatch().getBatchNumber());
                                                return cDto;
                                        })
                                        .collect(Collectors.toList());
                        sDto.setCustodyInfoDTOs(custodyInfoDTOs);

                        return sDto;
                } catch (Exception e) {
                        e.printStackTrace();
                        throw new IllegalArgumentException("Error Getting Shipment Details: " + e.getMessage());
                }
        }

}
