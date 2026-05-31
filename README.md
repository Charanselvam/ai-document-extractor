# AI Document Extractor

Extracts structured JSON from any Excel file using 
Google Gemini AI,  regardless of column structure or format.

## Problem it solves
Traditional Excel parsers like Apache POI require hardcoded 
column indices. When customers send different Excel formats, 
the parser breaks.

This service sends Excel content to Gemini AI with a 
structured prompt and returns consistent JSON every time.

## How it works
1. Upload any .xlsx file
2. Service extracts content as plain text
3. Gemini AI interprets the structure
4. Returns clean JSON

## Tech stack
- Java 21
- Spring Boot 4.0.6
- Apache POI (Excel reading)
- Google Gemini API (AI extraction)

## Run locally
1. Get free Gemini API key from aistudio.google.com
2. Set environment variable: GEMINI_API_KEY=your-key
3. Run: mvn spring-boot:run
4. POST http://localhost:8080/api/extract/excel
   with form-data: file=your-excel.xlsx

## Example
Input:  Any Excel with data
Output: Structured JSON extracted by AI

## Why Gemini over Apache POI parsing?
Apache POI requires knowing exact row and column positions.
Real-world Excel files from different customers have 
different structures. Gemini understands context and meaning, not just positions.
