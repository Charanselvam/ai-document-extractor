package com.aiextractor.document_extractor.service;


import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ExcelTextExtractorService {

    public String extractAsText(MultipartFile file) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                sb.append("Sheet: ").append(sheet.getSheetName()).append("\n");

                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        cells.add(getCellValue(cell));
                    }
                    String rowText = String.join(" | ", cells).trim();
                    if (!rowText.isEmpty() && !rowText.replace("|", "").trim().isEmpty()) {
                        sb.append(rowText).append("\n");
                    }
                }
                sb.append("\n");
            }
        }

        log.info("Extracted {} characters from Excel", sb.length());
        return sb.toString();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                        ? cell.getDateCellValue().toString()
                        : formatNumber(cell.getNumericCellValue());
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> {
                    try {
                        switch (cell.getCachedFormulaResultType()) {
                            case NUMERIC -> {
                                yield formatNumber(cell.getNumericCellValue());
                            }
                            case STRING -> {
                                yield cell.getStringCellValue().trim();
                            }
                            case BOOLEAN -> {
                                yield String.valueOf(cell.getBooleanCellValue());
                            }
                            default -> {
                                yield "";
                            }
                        }
                    } catch (Exception e) {
                        yield "";
                    }
                }
                case ERROR -> "";
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        }
        return String.format("%.6f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}