package com.smarthire.backend.ai.analytics;

import com.smarthire.backend.interview.entity.Interview;
import com.smarthire.backend.interview.entity.InterviewEvaluation;
import com.smarthire.backend.interview.repository.InterviewEvaluationRepository;
import com.smarthire.backend.interview.repository.InterviewRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** Generates analytics PDF reports with safe multi-page rendering. */
@Service
public class AnalyticsPdfService {
    private final InterviewRepository interviewRepository;
    private final InterviewEvaluationRepository interviewEvaluationRepository;

    public AnalyticsPdfService(InterviewRepository interviewRepository, InterviewEvaluationRepository interviewEvaluationRepository) {
        this.interviewRepository = interviewRepository;
        this.interviewEvaluationRepository = interviewEvaluationRepository;
    }

    public byte[] generateAnalyticsReport(Long userId) throws IOException {
        List<Interview> interviews = interviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (interviews == null) interviews = List.of();

        try (PDDocument document = new PDDocument(); PdfPageWriter writer = new PdfPageWriter(document)) {
            writer.title("SmartHire AI Analytics Report");
            writer.line("User ID: " + userId);
            writer.line("Total Interviews: " + interviews.size());
            if (!interviews.isEmpty()) {
                writer.heading("Interview History");
                for (Interview interview : interviews) {
                    if (interview != null) {
                        writer.line("Interview #" + interview.getId() + " - " + safe(interview.getJobRole(), "AI Mock Interview") + " - " + (interview.getCreatedAt() != null ? interview.getCreatedAt().toString() : ""));
                    }
                }
            }
            writer.heading("Scores");
            for (Interview interview : interviews) {
                if (interview != null && interview.getId() != null) {
                    InterviewEvaluation evaluation = interviewEvaluationRepository.findByInterviewId(interview.getId()).orElse(null);
                    if (evaluation != null) {
                        writer.line("Interview #" + interview.getId() + " Overall: " + evaluation.getOverallScore() + "%");
                    }
                }
            }
            writer.heading("Trends");
            if (interviews.size() >= 2) {
                int firstScore = getScore(interviews.get(interviews.size() - 1));
                int lastScore = getScore(interviews.get(0));
                String trend = lastScore > firstScore ? "Improving" : (lastScore < firstScore ? "Declining" : "Stable");
                writer.paragraph("Score trend: " + trend + " (from " + firstScore + "% to " + lastScore + "%)");
            } else {
                writer.paragraph("Not enough interview data to determine a trend.");
            }
            writer.heading("AI Feedback");
            if (!interviews.isEmpty() && interviews.get(0) != null && interviews.get(0).getId() != null) {
                InterviewEvaluation evaluation = interviewEvaluationRepository.findByInterviewId(interviews.get(0).getId()).orElse(null);
                if (evaluation != null) {
                    writer.paragraph("Recommendation: " + safe(evaluation.getRecommendation(), "Not available"));
                    if (evaluation.getStrengths() != null && !evaluation.getStrengths().isBlank()) {
                        writer.paragraph("Strengths: " + evaluation.getStrengths());
                    }
                } else {
                    writer.paragraph("No completed evaluation is available for the latest interview.");
                }
            } else {
                writer.paragraph("No interview feedback available yet.");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    public byte[] generateInterviewReport(Long interviewId) throws IOException {
        Interview interview = interviewRepository.findById(interviewId).orElseThrow(() -> new IllegalArgumentException("Interview not found: " + interviewId));
        InterviewEvaluation evaluation = interviewEvaluationRepository.findByInterviewId(interviewId).orElse(null);
        try (PDDocument document = new PDDocument(); PdfPageWriter writer = new PdfPageWriter(document)) {
            writer.title("SmartHire AI Interview Report");
            writer.line("Interview ID: " + interviewId);
            writer.line("Candidate User ID: " + interview.getUserId());
            writer.line("Role: " + safe(interview.getJobRole(), "Not provided"));
            writer.line("Interview Type: " + safe(interview.getInterviewType(), "Not provided"));
            writer.line("Created: " + (interview.getCreatedAt() != null ? interview.getCreatedAt().toString() : ""));
            writer.heading("Evaluation Scores");
            if (evaluation == null) {
                writer.paragraph("No completed evaluation is available for this interview.");
            } else {
                writer.line("Overall: " + n(evaluation.getOverallScore()) + "%");
                writer.line("Communication: " + n(evaluation.getCommunicationScore()) + "%");
                writer.line("Confidence: " + n(evaluation.getConfidenceScore()) + "%");
                writer.line("Technical Relevance: " + n(evaluation.getTechnicalScore()) + "%");
                writer.line("Professionalism: " + n(evaluation.getProfessionalismScore()) + "%");
                writer.line("Problem Solving: " + n(evaluation.getProblemSolvingScore()) + "%");
                writer.line("Rating: " + safe(evaluation.getRating(), "Pending"));
                writer.heading("AI Feedback");
                writer.paragraph("Recommendation: " + safe(evaluation.getRecommendation(), "Not available"));
                writer.paragraph("Strengths: " + safe(evaluation.getStrengths(), "Not available"));
                writer.paragraph("Weaknesses: " + safe(evaluation.getWeaknesses(), "Not available"));
                writer.paragraph("Improvement Suggestions: " + safe(evaluation.getImprovementSuggestions(), "Not available"));
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }

    private int getScore(Interview interview) {
        if (interview == null || interview.getId() == null) return 0;
        return interviewEvaluationRepository.findByInterviewId(interview.getId()).map(e -> n(e.getOverallScore())).orElse(0);
    }
    private int n(Integer v) { return v == null ? 0 : v; }
    private String safe(String value) { return safe(value, ""); }
    private String safe(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
}
