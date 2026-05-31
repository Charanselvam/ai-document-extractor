package com.aiextractor.document_extractor.controller;

import com.aiextractor.document_extractor.service.ExcelTextExtractorService;
import com.aiextractor.document_extractor.service.GeminiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/extract")
public class DocumentExtractorController {

    private final ExcelTextExtractorService extractorService;
    private final GeminiService geminiService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentExtractorController(
            ExcelTextExtractorService extractorService,
            GeminiService geminiService) {
        this.extractorService = extractorService;
        this.geminiService = geminiService;
    }

    @PostMapping(
            value = "/excel",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> extractFromExcel(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Please select a file");
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
                return ResponseEntity.badRequest()
                        .body("Only .xlsx files supported");
            }

            String excelText = extractorService.extractAsText(file);
            String jsonString = geminiService.extractQuotationJson(excelText);

            ObjectMapper mapper = new ObjectMapper();
            Object json = mapper.readValue(jsonString, Object.class);

            return ResponseEntity.ok(json);

        } catch (Exception e) {
            log.error("Extraction failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}