package com.karmika.hrms.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.karmika.hrms.dto.ResumeValidationResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Resume Validation Utility
 * Validates resume content and extracts key information
 */
@Component
@RequiredArgsConstructor
public class ResumeValidator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Common email regex pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@(.+)$"
    );

    // Phone pattern (basic)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:^|\\s)(?:\\+?91|0)?[6-9]\\d{9}(?:\\s|$)|(?:\\+\\d{1,3})?\\d{10,}"
    );

    // Experience pattern (years of experience)
    private static final Pattern EXPERIENCE_PATTERN = Pattern.compile(
            "(?:^|\\s)(\\d+)\\s*\\+?\\s*(?:years?|yrs?)\\s+(?:of\\s+)?(?:experience|exp)"
    );

    // Degree patterns
    private static final Set<String> DEGREE_KEYWORDS = Set.of(
            "bachelor", "b.tech", "b.e", "btech", "be",
            "master", "m.tech", "m.s", "mtech", "ms", "mba",
            "diploma", "phd", "doctorate",
            "bca", "mca", "bsc", "msc"
    );

    /**
     * Validate resume content
     */
    public ResumeValidationResultDTO validateResume(String resumeContent, JobRequirements requirements) {
        ResumeValidationResultDTO result = new ResumeValidationResultDTO();
        List<String> errors = new ArrayList<>();

        // Basic validations
        if (resumeContent == null || resumeContent.trim().isEmpty()) {
            errors.add("Resume content is empty");
            result.setIsValid(false);
            result.setQualityScore(0);
            result.setValidationStatus("INVALID");
            return result;
        }

        String resumeText = resumeContent.toLowerCase();

        // Check for essential information
        if (!containsEmail(resumeText)) {
            errors.add("No email address found in resume");
        }

        if (!containsPhone(resumeText)) {
            errors.add("No phone number found in resume");
        }

        // Extract experience
        int extractedExperience = extractExperience(resumeText);
        result.setExtractedExperienceYears(extractedExperience);

        // Check experience requirement
        if (requirements.getMinExperienceYears() > 0 && extractedExperience < requirements.getMinExperienceYears()) {
            errors.add("Experience (" + extractedExperience + " years) is below minimum requirement (" + requirements.getMinExperienceYears() + " years)");
        }

        // Check for education
        String qualification = extractEducation(resumeText);
        result.setHighestQualification(qualification);

        if (qualification == null || qualification.isEmpty()) {
            errors.add("No educational qualification found");
        }

        // Match required skills
        Map<String, Boolean> skillsMatch = matchSkills(resumeText, requirements.getRequiredSkills());
        result.setRequiredSkillsMatch(serializeMap(skillsMatch));

        List<String> missingSkills = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : skillsMatch.entrySet()) {
            if (!entry.getValue()) {
                missingSkills.add(entry.getKey());
            }
        }

        if (!missingSkills.isEmpty()) {
            errors.add("Missing required skills: " + String.join(", ", missingSkills));
        }

        // Find additional skills
        List<String> additionalSkills = findAdditionalSkills(resumeText, requirements.getRequiredSkills());
        result.setAdditionalSkillsFound(serializeList(additionalSkills));

        // Calculate quality score
        int qualityScore = calculateQualityScore(resumeText, extractedExperience, errors.size(), skillsMatch);
        result.setQualityScore(qualityScore);

        // Determine validation status
        result.setValidationStatus(errors.isEmpty() ? "VALID" : (qualityScore >= 60 ? "NEEDS_REVIEW" : "INVALID"));
        result.setIsValid(errors.isEmpty());
        result.setValidationErrors(serializeList(errors));

        // Generate recommendation
        generateRecommendation(result);

        // Extract summary
        result.setResumeSummary(extractResumeSummary(resumeText));

        return result;
    }

    private boolean containsEmail(String text) {
        return EMAIL_PATTERN.matcher(text).find();
    }

    private boolean containsPhone(String text) {
        return PHONE_PATTERN.matcher(text).find();
    }

    private int extractExperience(String text) {
        var matcher = EXPERIENCE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private String extractEducation(String text) {
        for (String degree : DEGREE_KEYWORDS) {
            if (text.contains(degree.toLowerCase())) {
                return degree.toUpperCase();
            }
        }
        return null;
    }

    private Map<String, Boolean> matchSkills(String text, List<String> requiredSkills) {
        Map<String, Boolean> matchedSkills = new LinkedHashMap<>();

        for (String skill : requiredSkills) {
            boolean found = text.contains(skill.toLowerCase());
            matchedSkills.put(skill, found);
        }

        return matchedSkills;
    }

    private List<String> findAdditionalSkills(String text, List<String> requiredSkills) {
        // Common IT/Technical skills to check
        Set<String> commonSkills = Set.of(
                "java", "python", "javascript", "typescript", "c++", "c#", "golang", "rust",
                "react", "angular", "vue", "spring boot", "spring", "hibernate", "jpa",
                "sql", "mysql", "postgresql", "mongodb", "cassandra", "redis",
                "aws", "azure", "gcp", "docker", "kubernetes", "jenkins",
                "git", "rest api", "graphql", "microservices", "agile", "scrum",
                "linux", "windows", "html", "css", "nodejs", "express",
                "ionic", "flutter", "android", "ios", "swift", "objective-c"
        );

        List<String> additional = new ArrayList<>();
        for (String skill : commonSkills) {
            if (text.contains(skill) && requiredSkills.stream()
                    .noneMatch(s -> s.equalsIgnoreCase(skill))) {
                additional.add(skill);
            }
        }

        return additional;
    }

    private int calculateQualityScore(String text, int experience, int errorCount, Map<String, Boolean> skillsMatch) {
        int score = 100;

        // Deduct for missing contact info
        score -= !containsEmail(text) ? 10 : 0;
        score -= !containsPhone(text) ? 10 : 0;

        // Deduct for missing education
        score -= extractEducation(text) == null ? 15 : 0;

        // Deduct for errors
        score -= Math.min(errorCount * 5, 30);

        // Deduct for missing skills
        long missingSkillsCount = skillsMatch.values().stream()
                .filter(matched -> !matched)
                .count();
        score -= Math.min((int) missingSkillsCount * 10, 20);

        // Bonus for experience
        if (experience >= 5) {
            score += 10;
        }

        return Math.max(score, 0);
    }

    private String extractResumeSummary(String text) {
        StringBuilder summary = new StringBuilder();

        // Extract basic info
        String email = extractEmail(text);
        if (email != null) {
            summary.append("Email: ").append(email).append("\n");
        }

        int experience = extractExperience(text);
        if (experience > 0) {
            summary.append("Experience: ").append(experience).append(" years\n");
        }

        String education = extractEducation(text);
        if (education != null) {
            summary.append("Education: ").append(education).append("\n");
        }

        return summary.toString();
    }

    private String extractEmail(String text) {
        var matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private void generateRecommendation(ResumeValidationResultDTO result) {
        if (!result.getIsValid()) {
            result.setRecommendation("REJECT");
            result.setRecommendationReason("Resume validation failed due to missing critical information");
            return;
        }

        int score = result.getQualityScore();

        if (score >= 80) {
            result.setRecommendation("SHORTLIST");
            result.setRecommendationReason("Resume meets all requirements with high quality score");
        } else if (score >= 60) {
            result.setRecommendation("MAYBE");
            result.setRecommendationReason("Resume meets basic requirements but may have some gaps");
        } else {
            result.setRecommendation("REJECT");
            result.setRecommendationReason("Resume quality score is below acceptable threshold");
        }
    }

    private String serializeMap(Map<String, ?> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String serializeList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    /**
     * Inner class for job requirements
     */
    public static class JobRequirements {
        private final int minExperienceYears;
        private final List<String> requiredSkills;

        public JobRequirements(int minExperienceYears, List<String> requiredSkills) {
            this.minExperienceYears = minExperienceYears;
            this.requiredSkills = requiredSkills != null ? requiredSkills : new ArrayList<>();
        }

        public int getMinExperienceYears() {
            return minExperienceYears;
        }

        public List<String> getRequiredSkills() {
            return requiredSkills;
        }
    }
}

