package com.sprboot.sprboot.service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sprboot.sprboot.dto.BatchDTO;
import com.sprboot.sprboot.dto.CustodyInfoDTO;
import com.sprboot.sprboot.dto.ProductDTO;
import com.sprboot.sprboot.dto.UnitDTO;
import com.sprboot.sprboot.dto.ProductRequest;
import com.sprboot.sprboot.dto.ProductInfoDTO;
import com.sprboot.sprboot.dto.ShipmentDTO;
import com.sprboot.sprboot.dto.UserDTO;
import com.sprboot.sprboot.dto.VerifyProductResponse;
import com.sprboot.sprboot.entity.Batch;
import com.sprboot.sprboot.entity.Product;
import com.sprboot.sprboot.entity.Unit;
import com.sprboot.sprboot.entity.Shipment;
import com.sprboot.sprboot.entity.User;
import com.sprboot.sprboot.repository.BatchRepository;
import com.sprboot.sprboot.repository.UnitRepository;
import com.sprboot.sprboot.repository.ProductRepository;
import com.sprboot.sprboot.repository.ShipmentRepository;
import com.sprboot.sprboot.repository.TraceabilityHistoryRepository;
import com.sprboot.sprboot.repository.UserRepository;
import com.sprboot.sprboot.utility.AESUtil;
import com.sprboot.sprboot.utility.HmacUtil;
import com.sprboot.sprboot.utility.MapperUtil;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final UnitRepository unitRepository;
    private final TraceabilityHistoryRepository traceabilityHistoryRepository;
    private final ShipmentRepository shipmentRepository;

    @Value("${frontend.product.path}")
    private String path;

    @Value("${sec.secretkeyhmac}")
    private String secretKeyHmac;

    @Value("${sec.secretkeyaes}")
    private String secretKeyAes;

    private DateTimeFormatter dateFormatter;

    private SecretKey key;

    public ProductService(ProductRepository productRepository, UserRepository userRepository,
            BatchRepository batchRepository, UnitRepository unitRepository,
            TraceabilityHistoryRepository traceabilityHistoryRepository, ShipmentRepository shipmentRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.unitRepository = unitRepository;
        this.traceabilityHistoryRepository = traceabilityHistoryRepository;
        this.shipmentRepository = shipmentRepository;
    }

    @PostConstruct
    public void init() {
        dateFormatter = DateTimeFormatter.ISO_DATE;
        key = AESUtil.getKeyFromString(secretKeyAes);
    }

    @Transactional
    public byte[] createProduct(ProductRequest productRequest) {

        User registrar = userRepository.findById(productRequest.getRegistrarID())
                .orElseThrow(() -> new RuntimeException("Registrar not found"));

        Set<String> productCodes = new HashSet<>(); // ex: [P123, P124]
        Map<String, Set<String>> productCodeToBatchNumbers = new HashMap<>(); // {P123=[B001, B002], P124[B003]}
        List<String> serialNumbers = new ArrayList<>();

        MultipartFile file = productRequest.getFile();
        String filename = file.getOriginalFilename();
        if (filename == null)
            throw new IllegalArgumentException("File name missing");

        // row in the file
        List<Map<String, String>> rows = new ArrayList<>();

        try {
            // handle CSV
            if (filename.endsWith(".csv")) {
                try (Reader reader = new InputStreamReader(file.getInputStream());
                        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                    for (CSVRecord record : parser) {
                        Map<String, String> row = new HashMap<>();
                        for (String header : parser.getHeaderMap().keySet()) {
                            row.put(header.trim(), record.get(header).trim());
                        }
                        rows.add(row);
                    }
                }
            }

            // handle excel
            else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
                    Sheet sheet = workbook.getSheetAt(0);
                    Iterator<Row> rowIterator = sheet.iterator();

                    // Read header row
                    Row headerRow = rowIterator.next();
                    List<String> headers = new ArrayList<>();
                    for (Cell cell : headerRow) {
                        headers.add(cell.getStringCellValue().trim());
                    }

                    // Read data rows
                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.next();
                        Map<String, String> rowData = new HashMap<>();
                        for (int i = 0; i < headers.size(); i++) {
                            Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                            String value = cell.toString().trim();
                            rowData.put(headers.get(i), value);
                        }
                        rows.add(rowData);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Parsing file failed: " + e.getMessage());
            throw new IllegalArgumentException("Parsing file failed: " + e.getMessage());
        }

        try {
            // 1) Validation
            for (Map<String, String> row : rows) {
                String productCode = getRequiredValue(row, "product_code");
                String batchNumber = getRequiredValue(row, "batch_number");
                String serialNumber = getRequiredValue(row, "serial_number");
                System.out.println("Product code -> " + productCode + ", Batch Number -> " + batchNumber
                        + ", Serial Number -> " + serialNumber);

                productCodes.add(productCode);
                productCodeToBatchNumbers
                        .computeIfAbsent(productCode, k -> new HashSet<>())
                        .add(batchNumber);
                serialNumbers.add(serialNumber);

            }

            System.out.println("ProductIDs -> " + productCodes);
            System.out.println("ProductBatchIDs -> " + productCodeToBatchNumbers);

            // validate serial numbers
            List<String> invalids = unitRepository.checkIfSerialNumberExist(serialNumbers);
            if (!invalids.isEmpty()) {
                throw new IllegalAccessException("The following serial numbers already exist: " + invalids);
            }

            // 1.1) Fetch all existing products
            List<Product> existingProducts = productRepository.findAllByProductCodes(registrar.getUserID(),
                    productCodes);
            Map<String, Product> productMap = existingProducts.stream()
                    .collect(Collectors.toMap(Product::getProductCode, p -> p));

            // 1.2) Fetch all existing batches under the products
            Set<String> allBatchNumbers = productCodeToBatchNumbers.values().stream().flatMap(Set::stream)
                    .collect(Collectors.toSet()); // ex: [B001, B002, B003]
            List<Batch> existingBatches = batchRepository.findAllByProductCodesAndBatchNumber(registrar.getUserID(),
                    productCodes, allBatchNumbers);
            // 1.3) Creating lookup map
            Map<String, Batch> batchMap = existingBatches.stream()
                    .collect(Collectors.toMap(
                            b -> b.getProduct().getProductCode() + "_" + b.getBatchNumber(),
                            b -> b));

            List<Product> savedProducts = new ArrayList<>();
            List<Unit> savedUnits = new ArrayList<>();

            // 2) Saving records
            for (Map<String, String> row : rows) {
                String productCode = getRequiredValue(row, "product_code");
                String productName = getRequiredValue(row, "product_name");
                String description = getRequiredValue(row, "description");
                String batchNumber = getRequiredValue(row, "batch_number");
                String serialNumber = getRequiredValue(row, "serial_number");

                Product product = productMap.get(productCode);
                Product savedProduct = null;
                if (product == null) {
                    // product not in db, create new
                    product = new Product();
                    product.setProductCode(productCode);
                    product.setProductName(productName);
                    product.setRegistrar(registrar);
                    product.setCreatedDate(LocalDate.now());
                    productMap.put(product.getProductCode(), product);
                    savedProduct = productRepository.save(product);
                }
                // product exist, check for batch
                String batchKey = product.getProductCode() + "_" + batchNumber;
                Batch batch = batchMap.get(batchKey);
                Batch savedBatch = null;
                if (batch == null) {
                    batch = new Batch();
                    batch.setBatchNumber(batchNumber);
                    batch.setProduct(product);
                    batchMap.put(batchKey, batch);
                    savedBatch = batchRepository.save(batch);
                }

                // valid product and batch, save unit
                savedUnits.add(saveUnit(product, batch, serialNumber, description, registrar));
            }

            String saveDir = "src/main/resources/product_qrcodes";
            filename = productRequest.getFilename();

            byte[] pdfBytes = PdfGeneratorService.generateQrCodePdf(savedUnits, saveDir, filename);

            return pdfBytes;
        } catch (Exception e) {
            System.out.println("Saving Product Exception: " + e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    private String getRequiredValue(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing or empty required field in: " + key);
        }
        return value.trim();
    }

    @Transactional
    private Unit saveUnit(Product product, Batch batch, String serialNumber, String description, User registrar) {
        // HMAC Hash param
        String productCode = product.getProductCode();
        String registrarID = String.valueOf(product.getRegistrar().getUserID());
        String batchNumber = batch.getBatchNumber();

        // convert to json
        String data = productCode + "|"
                + registrarID + "|"
                + batchNumber + "|"
                + serialNumber;
        System.out.println("kwww data = " + data);

        // generate HMAC Hash
        String hmacHash = HmacUtil.generateHmac(data, secretKeyHmac);
        System.out.println("kwww hmacHash = " + hmacHash);

        // saving Unit
        Unit unit = new Unit();
        unit.setSerialNumber(serialNumber);
        unit.setDescription(description);
        unit.setCreatedTimestamp(LocalDateTime.now());
        unit.setHMACHash(hmacHash);
        unit.setQrCodeURL("");
        unit.setBatch(batch);
        unit.setCurrentCustodianID(registrar.getUserID());
        Unit savedUnit = unitRepository.save(unit);

        // encrypt unit id and hmac hash
        Long unitID = savedUnit.getUnitID();
        String input = String.valueOf(unitID) + "|" + hmacHash;
        String encrypted = null;
        try {
            encrypted = AESUtil.encrypt(input, key);
            System.out.println("Encrypted = " + encrypted);
        } catch (Exception e) {
            System.out.println("[Product Service] Exception = " + e.getMessage());
        }

        // constructing QR Code URL
        String url = null;
        if (encrypted != null) {
            url = path + encrypted;
        }

        // save url to db
        int result = unitRepository.updateQrCodeUrl(url, unitID);
        savedUnit.setQrCodeURL(url);
        if (result < 1) {
            throw new IllegalArgumentException("Url of unit " + unitID + " cannot be updated");
        }

        return savedUnit;
    }

    public VerifyProductResponse verifyProduct(String cipherText) {
        // decrypt
        SecretKey key = AESUtil.getKeyFromString(secretKeyAes);

        String decrypt = null;
        Long unitID = null;
        String hashFromQR = null;

        // decrypt cipher Text
        try {
            decrypt = AESUtil.decrypt(cipherText, key);
        } catch (Exception e) {
            System.out.println("[Product Service] Exception = " + e.getMessage());
        }

        // extract unit and hash
        if (decrypt != null) {
            String[] parts = decrypt.split("\\|");
            unitID = Long.valueOf(parts[0]);
            hashFromQR = parts[1];
        }

        // find the unit record with unitID
        Unit unit = unitRepository.findById(unitID)
                .orElseThrow(() -> new IllegalArgumentException("Product Unit ID not found"));

        // verify if hmac hash match
        if (!unit.getHMACHash().equals(hashFromQR)) {
            throw new IllegalArgumentException("Hash value does not match");
        }

        VerifyProductResponse verifyProductResponse = new VerifyProductResponse();
        verifyProductResponse.setSerialNumber(unit.getSerialNumber());
        verifyProductResponse.setBatchNumber(unit.getBatch().getBatchNumber());
        verifyProductResponse.setProductCode(unit.getBatch().getProduct().getProductCode());
        verifyProductResponse.setProductName(unit.getBatch().getProduct().getProductName());
        verifyProductResponse.setDescription(unit.getDescription());

        // get traceability information
        List<Shipment> shipments = traceabilityHistoryRepository.findTraceabilityHistory(unitID);
        List<ShipmentDTO> shipmentDTOs = new ArrayList<>();

        // registrar
        ShipmentDTO registrarSDto = new ShipmentDTO();
        registrarSDto.setReceiverUsername(unit.getBatch().getProduct().getRegistrar().getUsername());
        registrarSDto.setReceivedTimestamp(unit.getCreatedTimestamp());
        shipmentDTOs.add(registrarSDto);

        // transfer participants
        if (!shipments.isEmpty()) {
            for (Shipment shipment : shipments) {
                ShipmentDTO sDto = new ShipmentDTO();
                sDto.setReceiverUsername(shipment.getReceiver().getUsername());
                sDto.setReceivedTimestamp(shipment.getReceivedTimestamp());
                shipmentDTOs.add(sDto);
            }
        }
        verifyProductResponse.setTraceRoute(shipmentDTOs);

        return verifyProductResponse;
    }

    // for My Custody Self Registered
    public List<CustodyInfoDTO> getMyCustodySelfRegistered(Long userID) {

        List<Unit> units = unitRepository.findByRegistrar(userID);
        List<CustodyInfoDTO> result = new ArrayList<>();

        for (Unit u : units) {
            CustodyInfoDTO dto = buildCustodyInfo(u, userID);
            // For self-registered items, override date
            if (dto.getCreatedReceivedDate() == null) {
                dto.setCreatedReceivedDate(u.getCreatedTimestamp());
            }
            result.add(dto);
        }

        return result;
    }

    // for My Custody Received
    public List<CustodyInfoDTO> getMyCustodyReceived(Long userID) {

        List<Unit> units = unitRepository.findByCustodian(userID);
        List<CustodyInfoDTO> result = new ArrayList<>();

        for (Unit u : units) {
            CustodyInfoDTO dto = buildCustodyInfo(u, userID);
            result.add(dto);
        }
        return result;
    }

    // for dashboard
    public List<ProductInfoDTO> getMyRegisteredProductSummary(Long userID) {
        List<ProductInfoDTO> summaryDTOs = new ArrayList<>();

        try {
            summaryDTOs = productRepository.getProductSummary(userID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return summaryDTOs;
    }

    // for dashboard
    public List<CustodyInfoDTO> getMyCustodySummary(Long userID) {
        List<Unit> units = unitRepository.getCustodySummary(userID);
        List<CustodyInfoDTO> result = new ArrayList<>();

        for (Unit u : units) {
            CustodyInfoDTO dto = buildCustodyInfo(u, userID);
            result.add(dto);
        }

        return result;
    }

    private CustodyInfoDTO buildCustodyInfo(Unit u, Long userID) {
        CustodyInfoDTO dto = new CustodyInfoDTO();
        dto.setSerialNumber(u.getSerialNumber());
        dto.setProductCode(u.getBatch().getProduct().getProductCode());
        dto.setProductName(u.getBatch().getProduct().getProductName());
        dto.setBatchNumber(u.getBatch().getBatchNumber());
        dto.setDescription(u.getDescription());
        dto.setQrCodeUrl(u.getQrCodeURL());

        // Find who sent this unit to the current user (receivedFrom)
        Shipment s1 = shipmentRepository.findShipmentSenderByUnit(u.getUnitID(), userID);
        if (s1 != null && s1.getSender() != null) {
            dto.setReceivedFrom(s1.getSender().getUsername());
            if (s1.getReceivedTimestamp() != null) {
                dto.setCreatedReceivedDate(s1.getReceivedTimestamp());
            }
        }

        // Find who current user sent this unit to (transferredTo)
        Shipment s2 = shipmentRepository.findShipmentReceiverByUnit(u.getUnitID(), userID);
        if (s2 != null && s2.getReceiver() != null) {
            dto.setTransferredTo(s2.getReceiver().getUsername());
        } else {
            dto.setTransferredTo("-");
        }

        // Determine status
        if (u.getStatus().equals("received") && (s2 == null || s2.getReceiver() == null)) {
            dto.setStatus("in custody");
        } else {
            // for case like created, pending, shipped
            dto.setStatus(u.getStatus());
        }

        return dto;
    }

    public ProductDTO getProductDetails(Long productID) {
        try {
            Product product = productRepository.getProductDetails(productID);
            return MapperUtil.toHierarchy(product);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Get Product Details Error: " + e.getMessage());
        }
    }

}
