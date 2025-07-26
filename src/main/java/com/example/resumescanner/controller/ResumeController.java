package com.example.resumescanner.controller;

import com.example.resumescanner.service.AIService;
import com.example.resumescanner.service.ResumeService;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Controller
@EnableAsync
public class ResumeController {
    // Predefined skill sets for roles
    private static final Map<String, List<String>> roleSkillMap = new HashMap<>();

    static {
        roleSkillMap.put("Java Developer", Arrays.asList("Java", "Spring Boot", "SQL", "Hibernate", "REST"));
        roleSkillMap.put("Frontend Developer", Arrays.asList("HTML", "CSS", "JavaScript", "Bootstrap", "React.js"));
        roleSkillMap.put("Full Stack Developer", Arrays.asList("Java", "Spring Boot", "HTML", "CSS", "JavaScript", "React.js", "SQL", "Node.js"));
        roleSkillMap.put("Data Analyst", Arrays.asList("Python", "MS Excel", "SQL", "Power BI"));
        roleSkillMap.put("Backend Developer", Arrays.asList("Java", "Spring Boot", "Hibernate", "REST API", "SQL"));
        roleSkillMap.put("UI/UX Designer", Arrays.asList(
                "Figma", "Adobe XD", "Sketch", "User Research", "Wire framing",
                "Prototyping", "Visual Design", "HTML", "CSS", "Design Thinking", "Typography"
        ));
        roleSkillMap.put("Data Scientist", Arrays.asList(
                "Python", "R", "SQL", "Machine Learning", "Statistics", "Pandas", "NumPy", "Matplotlib",
                "Scikit-learn", "TensorFlow", "Data Visualization", "Deep Learning", "Data Cleaning"
        ));
        roleSkillMap.put("Accountant", Arrays.asList(
                "Tally", "MS Excel", "GST", "TDS", "Taxation", "Accounting Standards",
                "Balance Sheet", "Profit & Loss", "Bank Reconciliation", "Financial Reporting", "SAP"
        ));
        roleSkillMap.put("Human Resource (HR)", Arrays.asList(
                "Recruitment", "Employee Engagement", "Payroll", "HRMS", "Excel",
                "Communication", "Onboarding", "Compliance", "Training & Development", "Performance Management"
        ));
        roleSkillMap.put("Sales Executive / Manager", Arrays.asList(
                "Lead Generation", "Cold Calling", "Negotiation", "CRM Tools",
                "Communication", "Sales Pitching", "Client Handling", "MS Excel", "Sales Forecasting"
        ));
        roleSkillMap.put("Digital Marketer", Arrays.asList(
                "SEO", "SEM", "Google Ads", "Facebook Ads", "Analytics", "Email Marketing",
                "Content Marketing", "Social Media", "Keyword Research", "Canva", "WordPress"
        ));
        roleSkillMap.put("Content Writer", Arrays.asList(
                "SEO Writing", "Grammar", "Content Research", "Blog Writing",
                "Copywriting", "Proofreading", "WordPress", "Content Strategy", "Creative Writing"
        ));
        roleSkillMap.put("Business Analyst", Arrays.asList(
                "Excel", "SQL", "Business Analysis", "Requirement Gathering",
                "UML", "Process Mapping", "Agile", "JIRA", "Power BI", "Communication"
        ));
        roleSkillMap.put("Customer Support Executive", Arrays.asList("Communication", "CRM Tools", "Problem Solving", "Email Etiquette",
                "Call Handling", "Active Listening", "Typing Skills", "MS Office", "Teamwork"));
    }

    @Autowired
    public AIService aiService;
    @Autowired
    public ResumeService resumeService;

    @RequestMapping("/")
    public String home(Model model) {
        model.addAttribute("roles", roleSkillMap.keySet());
        return "upload";
    }

    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        model.addAttribute("roles", roleSkillMap.keySet());
        return "upload";
    }

    @PostMapping("/upload")
    public String handleUpload(@RequestParam("resumeFile") MultipartFile file,
                               @RequestParam("jobRole") String jobRole,
                               Model model) {
        try {
            String resumeText = resumeService.extractTextFromFile(file);
            List<String> requiredSkills = roleSkillMap.getOrDefault(jobRole, new ArrayList<>());

            Map<String, Boolean> matchMap = resumeService.matchKeywords(resumeText, requiredSkills);

            List<String> matched = new ArrayList<>();
            List<String> missing = new ArrayList<>();

            for (Map.Entry<String, Boolean> entry : matchMap.entrySet()) {
                if (entry.getValue()) matched.add(entry.getKey());
                else missing.add(entry.getKey());
            }

            // Call AI services asynchronously
            CompletableFuture<String> experienceSummary = aiService.getSummary(resumeText);
            CompletableFuture<String> resumeStrength = aiService.getStrengthFeedback(resumeText);
            CompletableFuture<Integer> ratingSkills = aiService.getSkillsOutOf10(resumeText, jobRole);
            CompletableFuture<Integer> ratingExperience = aiService.getExperienceOutOf10(resumeText, jobRole);
            CompletableFuture<Integer> ratingOverall = aiService.getOverallOutOf10(resumeText);
            CompletableFuture<String> suggestionImp = aiService.suggestionImprove(resumeText);

            // Format ratings for display as "X/10"
            String skillsRating = ratingSkills.get() + "/10";
            String experienceRating = ratingExperience.get() + "/10";
            String overallRating = ratingOverall.get() + "/10";

            model.addAttribute("matchResult", Map.ofEntries(
                    Map.entry("matched", matched),
                    Map.entry("missing", missing),
                    Map.entry("matchCount", matched.size()),
                    Map.entry("totalKeywords", requiredSkills.size()),
                    Map.entry("selectedRole", jobRole),
                    Map.entry("experienceSummary", experienceSummary.get()),
                    Map.entry("resumeStrength", resumeStrength.get()),
                    Map.entry("ratingSkills", skillsRating),
                    Map.entry("ratingExperience", experienceRating),
                    Map.entry("ratingOverall", overallRating),
                    Map.entry("suggestionImp", suggestionImp.get())
            ));

            model.addAttribute("resumeText", resumeText);
            model.addAttribute("result", matchMap);

            return "result";
        } catch (IOException | TikaException e) {
            model.addAttribute("error", "Upload failed: " + e.getMessage());
            return "upload";
        } catch (ExecutionException | InterruptedException e) {
            model.addAttribute("error", "AI processing failed: " + e.getMessage());
            return "upload";
        }
    }
}