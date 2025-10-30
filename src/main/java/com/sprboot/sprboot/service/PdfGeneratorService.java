package com.sprboot.sprboot.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.sprboot.sprboot.entity.Shipment;
import com.sprboot.sprboot.entity.Unit;
import com.sprboot.sprboot.utility.QRCodeUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PdfGeneratorService {

    public static byte[] generateQrCodePdf(List<Unit> units, String saveDirectory, String fileName)
            throws Exception {
        if (units == null || units.isEmpty()) {
            throw new IllegalArgumentException("No units provided for QR PDF generation");
        }

        // Create directory if not exists
        Path dirPath = Path.of(saveDirectory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        Path pdfPath = dirPath.resolve(fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc, PageSize.A4)) {

            float[] columnWidths = { 1, 1, 1 }; // 3 columns per row
            Table table = new Table(columnWidths);
            table.setWidth(UnitValue.createPercentValue(100));

            int count = 0;

            for (Unit unit : units) {
                BufferedImage qrImage = QRCodeUtil.generateQRCodeImage(unit.getQrCodeURL(), 150, 150);
                ByteArrayOutputStream qrOut = new ByteArrayOutputStream();
                ImageIO.write(qrImage, "png", qrOut);
                Image qrPdfImage = new Image(ImageDataFactory.create(qrOut.toByteArray()));
                qrPdfImage.setAutoScale(true);

                String productName = unit.getBatch().getProduct().getProductName();
                String batchNumber = unit.getBatch().getBatchNumber();
                String serialNumber = unit.getSerialNumber();

                // build text below QR
                Paragraph text = new Paragraph(productName + "\n"
                        + "Batch: " + batchNumber + "\n"
                        + "Serial: " + serialNumber)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(9);

                Cell cell = new Cell()
                        .add(qrPdfImage)
                        .add(text)
                        .setTextAlignment(TextAlignment.CENTER);

                table.addCell(cell);
                count++;

                // add page after every 6 QR codes (3x2 layout)
                if (count % 6 == 0) {
                    document.add(table);
                    table = new Table(columnWidths);
                    table.setWidth(UnitValue.createPercentValue(100));
                }
            }

            // Add remaining if not full page
            if (count % 6 != 0) {
                document.add(table);
            }
        }

        // save to disk
        Files.write(pdfPath, baos.toByteArray());

        return baos.toByteArray();
    }

    public static byte[] generateQrCodePdf(Shipment shipment, String saveDirectory, String fileName)
            throws Exception {
        if (shipment == null) {
            throw new IllegalArgumentException("No shipment provided for QR PDF generation");
        }

        // Create directory if not exists
        Path dirPath = Path.of(saveDirectory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        Path pdfPath = dirPath.resolve(fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc, PageSize.A4)) {

            float[] columnWidths = { 1, 1, 1 };
            Table table = new Table(columnWidths);
            table.setWidth(UnitValue.createPercentValue(100));

            BufferedImage qrImage = QRCodeUtil.generateQRCodeImage(shipment.getQrCodeUrl(), 150, 150);
            ByteArrayOutputStream qrOut = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "png", qrOut);
            Image qrPdfImage = new Image(ImageDataFactory.create(qrOut.toByteArray()));

            qrPdfImage.setWidth(150);
            qrPdfImage.setHeight(150);
            qrPdfImage.setHorizontalAlignment(HorizontalAlignment.CENTER);

            String senderName = shipment.getSender().getUsername();
            String receiverName = shipment.getReceiver().getUsername();
            String shipmentID = String.valueOf(shipment.getShipmentID());

            // build text below QR
            Paragraph text = new Paragraph("Shipment ID: " + shipmentID + "\n"
                    + "From: " + senderName + "\n"
                    + "To: " + receiverName)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(9);

            Cell cell = new Cell()
                    .add(qrPdfImage)
                    .add(text)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(10f);

            table.addCell(cell);

            document.add(table);
        }

        // save to disk
        Files.write(pdfPath, baos.toByteArray());

        return baos.toByteArray();
    }
}
