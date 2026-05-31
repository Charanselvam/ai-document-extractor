package com.aiextractor.document_extractor.service;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();


    public String extractQuotationJson(String excelText) throws Exception {
        String prompt = buildPrompt(excelText);
        String requestBody = buildRequestBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error: " + response.body());
        }

        // Extract text from Gemini response
        JsonNode root = objectMapper.readTree(response.body());
        String extractedText = root
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

        // Clean markdown if present


        log.info("Gemini extracted JSON: {}", extractedText);
        return extractedText
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }

    private String buildPrompt(String excelText) {
        return """
                You are a data extraction specialist for manufacturing quotation documents.
                Extract quotation data from the provided Excel content and return ONLY a JSON object.
                No explanation, no markdown, no preamble. Just the raw JSON.
                
                Column names may vary — use context and meaning, not exact names.
                
                Return this exact structure:
                {
                  "projectCode": "string — project or inquiry code",
                  "tariff_percentage": "decimal — tariff as decimal e.g. 12.5% = 0.125",
                  "hs_code": "string or null",
                  "quotation": [
                    {
                      "vendorName": "string",
                      "partNumber": "string",
                      "material": "string",
                      "quantity": "integer",
                      "fob": "decimal",
                      "tooling": "decimal or 0",
                      "drawingNumber": "string or null",
                      "rev": "string or null",
                      "description": "string",
                      "weight": "decimal or 0",
                      "quotation_date": "MM/DD/YY format"
                    }
                  ],
                  "selected_quotation": [
                    {
                      "vendorName": "string",
                      "partNumber": "string",
                      "material": "string",
                      "quantity": "integer",
                      "Freight": "decimal or 0"
                    }
                  ]
                }
                
                Rules:
                1. Return ONLY the JSON. No markdown, no explanation.
                2. Missing string fields use empty string "" never null.
                3. Missing number fields use 0 never null.
                4. If extraction fails return: {"error": "reason"}
                5. Dates must be MM/DD/YY format.
                6. tariff_percentage as decimal — 12.5% becomes 0.125
                7. description field is required — if not found use partNumber as description.
                
                Excel content:
                """ + excelText;
    }

    private String buildRequestBody(String prompt) throws Exception {
        return objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .set("contents", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode()
                                        .set("parts", objectMapper.createArrayNode()
                                                .add(objectMapper.createObjectNode()
                                                        .put("text", prompt)))))
        );
    }
}