package com.example.resumescanner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@EnableAsync
public class AIService {

    private final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    @Value("${groq.api.key}")
    private String apiKey;


    private String callGroq(String prompt) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            Map<String, Object> body = new HashMap<>();
            body.put("model", "llama3-8b-8192"); // Use Mixtral model
            body.put("temperature", 0.7);

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", prompt)
            );
            body.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey); // Add Authorization header

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

            if (response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return message.get("content").toString().trim();
                }
            }
            return "No response from Groq.";
        } catch (Exception e) {
            System.err.println("Error calling Groq: " + e.getMessage());
            return "Error occurred while contacting Groq.";
        }
    }

    private Integer extractRatingFromResponse(String response) {
        try {
            Pattern pattern = Pattern.compile("(\\d+)/10");
            Matcher matcher = pattern.matcher(response);

            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }

            pattern = Pattern.compile("\\b([1-9]|10)\\b");
            matcher = pattern.matcher(response);

            if (matcher.find()) {
                int rating = Integer.parseInt(matcher.group(1));
                if (rating >= 1 && rating <= 10) {
                    return rating;
                }
            }

            return 5;
        } catch (Exception e) {
            System.err.println("Error extracting rating: " + e.getMessage());
            return 5;
        }
    }

    @Async
    public CompletableFuture<String> getSummary(String resumeText) {
        String prompt = "Summarize the experience from this resume in 10–20 words:\n" + resumeText;
        return CompletableFuture.completedFuture(callGroq(prompt));
    }

    @Async
    public CompletableFuture<String> getStrengthFeedback(String resumeText) {
        String prompt = "Give resume feedback within 10–20 words:\n" + resumeText;
        return CompletableFuture.completedFuture(callGroq(prompt));
    }

    @Async
    public CompletableFuture<Integer> getSkillsOutOf10(String resumeText, String jobRole) {
        String prompt = "Rate this resume out of 10 for technical skills for the role of " + jobRole +
                ". Respond ONLY with a number like '8' or '8/10'. Here's the resume:\n" + resumeText;
        return CompletableFuture.completedFuture(extractRatingFromResponse(callGroq(prompt)));
    }

    @Async
    public CompletableFuture<Integer> getExperienceOutOf10(String resumeText, String jobRole) {
        String prompt = "Rate this resume out of 10 for experience relevance for the role of " + jobRole +
                ". Respond ONLY with a number like '7' or '7/10'. Here's the resume:\n" + resumeText;
        return CompletableFuture.completedFuture(extractRatingFromResponse(callGroq(prompt)));
    }

    @Async
    public CompletableFuture<Integer> getOverallOutOf10(String resumeText) {
        String prompt = "Rate this resume out of 10 based on overall strength and quality. " +
                "Respond ONLY with a number like '9' or '9/10'. Here's the resume:\n" + resumeText;
        return CompletableFuture.completedFuture(extractRatingFromResponse(callGroq(prompt)));
    }

    @Async
    public CompletableFuture<String> suggestionImprove(String resumeText) {
        String prompt = "Give suggestions to improve this resume in 3 bullet points:\n" + resumeText;
        return CompletableFuture.completedFuture(callGroq(prompt));
    }
}
