package com.example.resumescanner.service;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeService {

    public String extractTextFromFile(MultipartFile file) throws IOException, TikaException {
        if (file.isEmpty()) {
            return "No file uploaded."; // Or throw an exception
        }

        // --- FIX STARTS HERE ---
        // Copy the InputStream to a ByteArrayInputStream for robust parsing
        byte[] bytes = file.getBytes();
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();

            parser.parse(stream, handler, metadata);
            return handler.toString();
        } catch (SAXException e) {
            // SAXException can be thrown by the parser
            throw new TikaException("Error parsing document with Tika (SAXException)", e);
        }
        // --- FIX ENDS HERE ---
    }


    public Map<String, Boolean> matchKeywords(String resumeText, List<String> keywords) {
        Map<String, Boolean> matchMap = new java.util.HashMap<>();
        String lowerCaseResumeText = resumeText.toLowerCase();

        for (String keyword : keywords) {
            // Using Pattern.CASE_INSENSITIVE for the regex matching if resumeText is not already lowercased
            // But since lowerCaseResumeText is used, no need for Pattern.CASE_INSENSITIVE here in regex itself
            // but ensuring the keyword is also lowercased
            String lowerCaseKeyword = keyword.toLowerCase();
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(lowerCaseKeyword) + "\\b"); // \\b for word boundary
            Matcher matcher = pattern.matcher(lowerCaseResumeText);
            matchMap.put(keyword, matcher.find());
        }
        return matchMap;
    }
}