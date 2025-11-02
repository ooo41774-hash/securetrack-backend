package com.sprboot.sprboot.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
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

    public static byte[] generateQrCodePdf(List<Unit> units,
            String saveDirectory,
            String fileName) throws Exception {

        if (units == null || units.isEmpty()) {
            throw new IllegalArgumentException("No units provided for QR PDF generation");
        }

        // -----------------------------------------------------------------
        // 1. Create the directory if it does not exist
        // -----------------------------------------------------------------
        Path dirPath = Path.of(saveDirectory);
        Files.createDirectories(dirPath);
        Path pdfPath = dirPath.resolve(fileName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc, PageSize.A4)) {

            // -----------------------------------------------------------------
            // 2. Title of the first page
            // -----------------------------------------------------------------
            document.add(createTitle());

            // -----------------------------------------------------------------
            // 3. Table layout – 4 columns, 6 rows → 24 QR-codes per page
            // -----------------------------------------------------------------
            float[] columnWidths = { 1, 1, 1, 1 };
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            final int UNITS_PER_PAGE = 24;
            final int UNITS_PER_ROW = 4;
            int index = 0;
            int totalUnits = units.size();

            // -----------------------------------------------------------------
            // 4. Fill the table row-by-row
            // -----------------------------------------------------------------
            while (index < totalUnits) {

                // ---- start a new page when we have filled a full page ----
                if (index % UNITS_PER_PAGE == 0 && index > 0) {
                    addPageWithFooterAndTitle(document, table);
                    // fresh table for the new page
                    table = new Table(UnitValue.createPercentArray(columnWidths));
                    table.setWidth(UnitValue.createPercentValue(100));
                }

                // ---- one complete row (4 cells) ----
                for (int col = 0; col < UNITS_PER_ROW; col++) {
                    if (index < totalUnits) {
                        Unit unit = units.get(index++);

                        // ---- QR image -------------------------------------------------
                        BufferedImage qrImg = QRCodeUtil.generateQRCodeImage(
                                unit.getQrCodeURL(), 100, 100);
                        ByteArrayOutputStream qrOut = new ByteArrayOutputStream();
                        ImageIO.write(qrImg, "png", qrOut);

                        Cell cell = new Cell();
                        String productName = unit.getBatch().getProduct().getProductName();
                        String batchNumber = unit.getBatch().getBatchNumber();
                        String serialNumber = unit.getSerialNumber();

                        // 1. QR Image
                        Image qrPdfImg = new Image(ImageDataFactory.create(qrOut.toByteArray()))
                                .setWidth(100).setHeight(100)
                                .setHorizontalAlignment(HorizontalAlignment.CENTER)
                                .setMarginBottom(0f);
                        cell.add(qrPdfImg);

                        // 2. CTA
                        Paragraph cta = new Paragraph("Scan to view trace route")
                                .setTextAlignment(TextAlignment.CENTER)
                                .setFontSize(8).setItalic()
                                .setMarginTop(-8f).setMarginBottom(3f)
                                .setFixedLeading(8f);
                        cell.add(cta);

                        // 3. Horizontal Line
                        LineSeparator line = new LineSeparator(new SolidLine(0.5f))
                                .setMarginTop(2f)
                                .setMarginBottom(2f)
                                .setWidth(UnitValue.createPercentValue(100));

                        cell.add(new Paragraph().add(line).setMargin(0));

                        // 4. Reference Text
                        Paragraph ref = new Paragraph(
                                productName + "\nBatch: " + batchNumber + "\nSerial: " + serialNumber)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setFontSize(7)
                                .setMarginTop(2f)
                                .setFixedLeading(7f);
                        cell.add(ref);

                        // Final cell styling
                        cell.setTextAlignment(TextAlignment.CENTER)
                                .setPadding(5f)
                                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                                .setWidth(UnitValue.createPercentValue(25));

                        table.addCell(cell);
                    } else {
                        // ---- Empty placeholder (keeps column width) ----
                        Cell empty = new Cell()
                                .setWidth(UnitValue.createPercentValue(25))
                                .setPadding(5f)
                                .setBorder(Border.NO_BORDER);
                        table.addCell(empty);
                    }
                }
            }

            // -----------------------------------------------------------------
            // 5. Add the last (possibly partial) page
            // -----------------------------------------------------------------
            if (table.getNumberOfRows() > 0) {
                document.add(table);
                document.add(createFooter());
            }
        }

        // -----------------------------------------------------------------
        // 6. Write the PDF to disk and return the byte[]
        // -----------------------------------------------------------------
        byte[] pdfBytes = baos.toByteArray();
        // Files.write(pdfPath, pdfBytes); comment till knows where to save
        return pdfBytes;
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods */
    /* --------------------------------------------------------------------- */
    private static Paragraph createTitle() {
        return new Paragraph("Product QR Codes")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16)
                .setBold()
                .setMarginBottom(15);
    }

    private static Paragraph createFooter() {
        return new Paragraph("Please attach on product unit")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(9)
                .setMarginTop(10);
    }

    private static void addPageWithFooterAndTitle(Document document, Table table) {
        document.add(table);
        document.add(createFooter());
        document.add(new AreaBreak());
        document.add(createTitle());
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

            Paragraph title = new Paragraph("Shipment QR Codes")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(16)
                    .setBold()
                    .setMarginBottom(20);
            document.add(title);

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

            Paragraph footer = new Paragraph("Please attach on shipment package and scan to mark as dispatch")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10)
                    .setMarginTop(20);
            document.add(footer);
        }

        // save to disk
        // Files.write(pdfPath, baos.toByteArray()); comment till knows where to save

        return baos.toByteArray();
    }
}
