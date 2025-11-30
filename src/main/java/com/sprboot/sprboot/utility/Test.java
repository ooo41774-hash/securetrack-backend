package com.sprboot.sprboot.utility;

import java.util.ArrayList;
import java.util.List;

import com.sprboot.sprboot.entity.Unit;
import com.sprboot.sprboot.service.PdfGeneratorService;

public class Test {
    public static void main(String[] args) {
        List<Unit> units = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Unit unit = new Unit();
            unit.setQrCodeURL("hahahahahahahahahahaha");
            units.add(unit);
        }

        String saveDir = "src/main/resources/test";
        String filename = "filename";

        byte[] pdfBytes = null;
        try {
            pdfBytes = PdfGeneratorService.generateQrCodePdf(units, saveDir, filename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
