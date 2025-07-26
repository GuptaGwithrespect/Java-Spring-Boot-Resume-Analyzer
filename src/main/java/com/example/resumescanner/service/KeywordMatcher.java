package com.example.resumescanner.service;

import java.util.*;

public class KeywordMatcher {

    public static Map<String, Object> matchKeywords(String resumeText, List<String> keywords) {
        int matchedCount = 0;
        List<String> matchedKeywords = new ArrayList<>();

        for (String keyword : keywords) {
            if (resumeText.toLowerCase().contains(keyword.toLowerCase())) {
                matchedCount++;
                matchedKeywords.add(keyword);
            }
        }

        int totalKeywords = keywords.size();
        double percentage = (matchedCount * 100.0) / totalKeywords;

        Map<String, Object> result = new HashMap<>();
        result.put("matchedKeywords", matchedKeywords);
        result.put("totalMatched", Optional.of(matchedCount));
        result.put("matchPercentage", Optional.of(percentage));

        return result;
    }
}
