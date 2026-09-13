package com.smarthire.backend.platform.analytics.service;

import com.smarthire.backend.entity.Resume;
import com.smarthire.backend.entity.User;
import com.smarthire.backend.interview.entity.Interview;
import com.smarthire.backend.interview.entity.InterviewEvaluation;
import com.smarthire.backend.interview.repository.InterviewEvaluationRepository;
import com.smarthire.backend.interview.repository.InterviewRepository;
import com.smarthire.backend.platform.analytics.dto.CandidateAnalyticsResponse;
import com.smarthire.backend.platform.analytics.dto.RecruiterAnalyticsResponse;
import com.smarthire.backend.platform.entity.PlatformActionLog;
import com.smarthire.backend.platform.repository.PlatformActionLogRepository;
import com.smarthire.backend.repository.ResumeRepository;
import com.smarthire.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final UserRepository users;
    private final ResumeRepository resumes;
    private final InterviewRepository interviews;
    private final InterviewEvaluationRepository evaluations;
    private final PlatformActionLogRepository actions;

    public AnalyticsService(UserRepository users, ResumeRepository resumes, InterviewRepository interviews, InterviewEvaluationRepository evaluations, PlatformActionLogRepository actions) {
        this.users = users;
        this.resumes = resumes;
        this.interviews = interviews;
        this.evaluations = evaluations;
        this.actions = actions;
    }

    public CandidateAnalyticsResponse candidate(Long userId) {
        if (!users.existsById(userId)) {
            throw new IllegalArgumentException("Candidate not found: " + userId);
        }
        List<Interview> owned = interviews.findByUserIdOrderByCreatedAtDesc(userId);
        if (owned == null) owned = List.of();

        Map<Long, InterviewEvaluation> evals = new HashMap<>();
        for (Interview i : owned) {
            if (i != null && i.getId() != null) {
                evaluations.findByInterviewId(i.getId()).ifPresent(e -> evals.put(i.getId(), e));
            }
        }

        List<Interview> completed = owned.stream()
                .filter(i -> i != null && i.getId() != null && evals.containsKey(i.getId()))
                .collect(Collectors.toList());
        List<InterviewEvaluation> scored = completed.stream()
                .map(i -> evals.get(i.getId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        CandidateAnalyticsResponse out = new CandidateAnalyticsResponse();
        CandidateAnalyticsResponse.Summary s = out.getSummary();
        s.setCompletedInterviews(completed.size());
        s.setEvaluatedInterviews(scored.size());
        s.setAverageOverallScore(avg(scored, InterviewEvaluation::getOverallScore));
        s.setAverageCommunication(avg(scored, e -> n(e.getCommunicationScore())));
        s.setAverageConfidence(avg(scored, e -> n(e.getConfidenceScore())));
        s.setAverageTechnical(avg(scored, e -> n(e.getTechnicalScore())));
        s.setAverageProfessionalism(avg(scored, e -> n(e.getProfessionalismScore())));
        s.setAverageProblemSolving(avg(scored, e -> n(e.getProblemSolvingScore())));

        if (!completed.isEmpty()) {
            Interview latest = completed.get(0);
            InterviewEvaluation le = evals.get(latest.getId());
            InterviewEvaluation firstEval = evals.get(completed.get(completed.size() - 1).getId());

            int ls = le != null ? n(le.getOverallScore()) : 0;
            int fs = firstEval != null ? n(firstEval.getOverallScore()) : ls;

            s.setLatestOverallScore(ls);
            s.setImprovementDelta(ls - fs);
            s.setRating(nvl(le != null ? le.getRating() : null, rating(ls)));
            s.setLatestJobRole(nvl(latest.getJobRole(), "AI Mock Interview"));
            s.setLatestDate(latest.getCreatedAt());
            s.setReportAvailable(true);

            if (le != null) {
                out.setFeedback(feedback(le));
            }
        }

        List<CandidateAnalyticsResponse.HistoryItem> history = new ArrayList<>();
        List<CandidateAnalyticsResponse.TrendPoint> trend = new ArrayList<>();

        for (Interview i : owned) {
            if (i == null || i.getId() == null) continue;
            CandidateAnalyticsResponse.HistoryItem h = new CandidateAnalyticsResponse.HistoryItem();
            h.setInterviewId(i.getId());
            h.setJobRole(nvl(i.getJobRole(), "AI Mock Interview"));
            h.setInterviewType(nvl(i.getInterviewType(), "Interview"));
            h.setDate(i.getCreatedAt());

            InterviewEvaluation e = evals.get(i.getId());
            if (e == null) {
                h.setStatus("In progress / not evaluated");
            } else {
                int overall = n(e.getOverallScore());
                h.setStatus("Completed");
                h.setOverallScore(overall);
                h.setCommunicationScore(n(e.getCommunicationScore()));
                h.setConfidenceScore(n(e.getConfidenceScore()));
                h.setTechnicalScore(n(e.getTechnicalScore()));
                h.setProfessionalismScore(n(e.getProfessionalismScore()));
                h.setProblemSolvingScore(n(e.getProblemSolvingScore()));
                h.setRating(nvl(e.getRating(), rating(overall)));

                CandidateAnalyticsResponse.TrendPoint t = new CandidateAnalyticsResponse.TrendPoint();
                t.setDate(i.getCreatedAt());
                t.setOverall(overall);
                t.setCommunication(n(e.getCommunicationScore()));
                t.setConfidence(n(e.getConfidenceScore()));
                t.setTechnical(n(e.getTechnicalScore()));
                t.setProfessionalism(n(e.getProfessionalismScore()));
                t.setProblemSolving(n(e.getProblemSolvingScore()));
                trend.add(0, t);
            }
            history.add(h);
        }
        out.setHistory(history);
        out.setTrends(trend);

        out.setSkillAnalytics(List.of(
                new CandidateAnalyticsResponse.MetricItem("Communication", (int) Math.round(s.getAverageCommunication()), "Module 7 weighted communication evidence"),
                new CandidateAnalyticsResponse.MetricItem("Confidence", (int) Math.round(s.getAverageConfidence()), "Eye contact, attention and confidence evidence"),
                new CandidateAnalyticsResponse.MetricItem("Technical Relevance", (int) Math.round(s.getAverageTechnical()), "AI technical evaluation"),
                new CandidateAnalyticsResponse.MetricItem("Professionalism", (int) Math.round(s.getAverageProfessionalism()), "Time management, organization and etiquette"),
                new CandidateAnalyticsResponse.MetricItem("Problem Solving", (int) Math.round(s.getAverageProblemSolving()), "Technical problem-solving evidence")
        ));

        List<CandidateAnalyticsResponse.WeakArea> weak = new ArrayList<>();
        out.getSkillAnalytics().stream()
                .sorted(Comparator.comparingInt(CandidateAnalyticsResponse.MetricItem::getValue))
                .limit(3)
                .forEach(m -> {
                    if (!scored.isEmpty() || m.getValue() > 0) {
                        weak.add(new CandidateAnalyticsResponse.WeakArea(m.getLabel(), m.getValue(), "Lowest assessed interview dimension", "Interview evaluation"));
                    }
                });

        Resume r = findLatestResume(userId);
        if (r != null) {
            split(r.getMissingSkills()).stream().limit(5).forEach(skill ->
                    weak.add(new CandidateAnalyticsResponse.WeakArea(skill, 0, "Missing skill identified from resume analysis", "Resume analysis"))
            );
            int skillsEvidence = r.getSkillsScore() == null ? 0 : Math.max(0, Math.min(100, r.getSkillsScore()));
            out.setResumeSkills(split(r.getSkills()).stream().limit(12).map(x ->
                    new CandidateAnalyticsResponse.ResumeSkill(x, skillsEvidence, "Latest resume skill evidence")
            ).toList());
        }
        out.setWeakAreas(weak);

        if (completed.size() >= 2) {
            InterviewEvaluation first = evals.get(completed.get(completed.size() - 1).getId());
            InterviewEvaluation last = evals.get(completed.get(0).getId());
            if (first != null && last != null) {
                out.setImprovement(List.of(
                        improvement("Overall", n(first.getOverallScore()), n(last.getOverallScore())),
                        improvement("Communication", n(first.getCommunicationScore()), n(last.getCommunicationScore())),
                        improvement("Confidence", n(first.getConfidenceScore()), n(last.getConfidenceScore())),
                        improvement("Technical Relevance", n(first.getTechnicalScore()), n(last.getTechnicalScore())),
                        improvement("Professionalism", n(first.getProfessionalismScore()), n(last.getProfessionalismScore())),
                        improvement("Problem Solving", n(first.getProblemSolvingScore()), n(last.getProblemSolvingScore()))
                ));
            }
        }
        return out;
    }

    public RecruiterAnalyticsResponse recruiter() {
        List<User> candidates = users.findAll().stream()
                .filter(u -> u.getRole() != null && "candidate".equalsIgnoreCase(u.getRole().trim()))
                .toList();
        List<Resume> allResumes = resumes.findAll();
        List<Interview> allInterviews = interviews.findAll();

        Map<Long, List<Interview>> byUser = allInterviews.stream()
                .filter(i -> i.getUserId() != null)
                .collect(Collectors.groupingBy(Interview::getUserId));

        List<InterviewEvaluation> allE = allInterviews.stream()
                .map(i -> evaluations.findByInterviewId(i.getId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        Map<Long, InterviewEvaluation> evalByInterview = allE.stream()
                .collect(Collectors.toMap(InterviewEvaluation::getInterviewId, e -> e, (a, b) -> a));

        RecruiterAnalyticsResponse out = new RecruiterAnalyticsResponse();
        RecruiterAnalyticsResponse.Summary sum = out.getSummary();
        sum.setTotalCandidates(candidates.size());

        List<RecruiterAnalyticsResponse.CandidateItem> ranking = new ArrayList<>();
        for (User u : candidates) {
            List<Interview> its = byUser.getOrDefault(u.getId(), List.of());
            InterviewEvaluation latestEval = its.stream()
                    .sorted(Comparator.comparing(Interview::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(i -> evalByInterview.get(i.getId()))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            int interviewScore = latestEval == null ? 0 : n(latestEval.getOverallScore());
            int ats = findAts(u.getId(), allResumes);
            int readiness = (interviewScore == 0 && ats == 0) ? 0 : (int) Math.round(((interviewScore * 0.6) + (ats * 0.4)));

            RecruiterAnalyticsResponse.CandidateItem item = new RecruiterAnalyticsResponse.CandidateItem();
            item.setCandidateId(u.getId());
            item.setName(nvl(u.getName(), "Candidate"));
            item.setStatus(resolveCandidateStatus(u.getId(), its, evalByInterview));
            item.setInterviewScore(interviewScore);
            item.setAtsScore(ats);
            item.setReadiness(readiness);
            item.setLastInterview(latestEval == null ? null : its.stream()
                    .filter(i -> evalByInterview.containsKey(i.getId()))
                    .max(Comparator.comparing(Interview::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(Interview::getCreatedAt).orElse(null));
            ranking.add(item);
        }

        sum.setScoredCandidates((int) ranking.stream().filter(x -> x.getInterviewScore() > 0).count());
        sum.setAverageInterviewScore(avgInts(ranking, RecruiterAnalyticsResponse.CandidateItem::getInterviewScore));
        sum.setAverageAtsScore(avgInts(ranking, RecruiterAnalyticsResponse.CandidateItem::getAtsScore));
        sum.setShortlistedCandidates((int) ranking.stream().filter(x -> "Shortlisted".equalsIgnoreCase(x.getStatus())).count());
        sum.setShortlistRate(ranking.isEmpty() ? 0 : Math.round(sum.getShortlistedCandidates() * 100f / ranking.size()));

        ranking.sort(Comparator.comparingInt(RecruiterAnalyticsResponse.CandidateItem::getReadiness).reversed());
        out.setRanking(ranking.stream().limit(25).toList());

        out.setDimensionAverages(List.of(
                new RecruiterAnalyticsResponse.MetricItem("Communication", avg(allE, e -> n(e.getCommunicationScore()))),
                new RecruiterAnalyticsResponse.MetricItem("Confidence", avg(allE, e -> n(e.getConfidenceScore()))),
                new RecruiterAnalyticsResponse.MetricItem("Technical Relevance", avg(allE, e -> n(e.getTechnicalScore()))),
                new RecruiterAnalyticsResponse.MetricItem("Professionalism", avg(allE, e -> n(e.getProfessionalismScore()))),
                new RecruiterAnalyticsResponse.MetricItem("Problem Solving", avg(allE, e -> n(e.getProblemSolvingScore())))
        ));

        List<Interview> evaluated = allInterviews.stream()
                .filter(i -> evalByInterview.containsKey(i.getId()))
                .sorted(Comparator.comparing(Interview::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        out.setTrends(evaluated.stream().map(i -> {
            RecruiterAnalyticsResponse.TrendPoint t = new RecruiterAnalyticsResponse.TrendPoint();
            t.setDate(i.getCreatedAt());
            t.setScore(n(evalByInterview.get(i.getId()).getOverallScore()));
            return t;
        }).toList());

        out.setSkills(countSkills(allResumes, false));
        out.setWeakSkills(countSkills(allResumes, true));
        return out;
    }

    private List<RecruiterAnalyticsResponse.SkillCount> countSkills(List<Resume> rs, boolean weak) {
        Map<String, Integer> m = new HashMap<>();
        for (Resume r : rs) {
            for (String s : split(weak ? r.getMissingSkills() : r.getSkills())) {
                m.merge(s.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }
        return m.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(e -> new RecruiterAnalyticsResponse.SkillCount(e.getKey(), e.getValue()))
                .toList();
    }

    private String resolveCandidateStatus(Long candidateId, List<Interview> its, Map<Long, InterviewEvaluation> e) {
        List<PlatformActionLog> logs = actions.findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc("candidate", candidateId);
        for (PlatformActionLog log : logs) {
            String a = nvl(log.getActionType(), "").toLowerCase(Locale.ROOT);
            if (a.contains("shortlist")) return "Shortlisted";
            if (a.contains("reject")) return "Rejected";
        }
        if (its.stream().anyMatch(i -> e.containsKey(i.getId()))) return "Evaluated";
        if (!its.isEmpty()) return "Interview in progress";
        return "New";
    }

    private int findAts(Long uid, List<Resume> rs) {
        return rs.stream()
                .filter(r -> Objects.equals(r.getUserId(), uid))
                .map(Resume::getAtsScore)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private Resume findLatestResume(Long uid) {
        return resumes.findAll().stream()
                .filter(r -> Objects.equals(r.getUserId(), uid))
                .max(Comparator.comparing(Resume::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private CandidateAnalyticsResponse.Feedback feedback(InterviewEvaluation e) {
        CandidateAnalyticsResponse.Feedback f = new CandidateAnalyticsResponse.Feedback();
        f.setStrengths(split(e.getStrengths()));
        f.setWeaknesses(split(e.getWeaknesses()));
        f.setImprovementSuggestions(split(e.getImprovementSuggestions()));
        f.setPracticeRecommendations(split(e.getPracticeRecommendations()));
        f.setLearningResources(split(e.getLearningResources()));
        f.setRecommendation(nvl(e.getRecommendation(), ""));
        return f;
    }

    private CandidateAnalyticsResponse.ImprovementItem improvement(String l, int b, int n) {
        return new CandidateAnalyticsResponse.ImprovementItem(l, b, n);
    }

    private int avg(List<InterviewEvaluation> list, ToIntFunction<InterviewEvaluation> f) {
        return list.isEmpty() ? 0 : (int) Math.round(list.stream().mapToInt(f).average().orElse(0));
    }

    private int avgInts(List<RecruiterAnalyticsResponse.CandidateItem> list, ToIntFunction<RecruiterAnalyticsResponse.CandidateItem> f) {
        return list.isEmpty() ? 0 : (int) Math.round(list.stream().mapToInt(f).filter(v -> v > 0).average().orElse(0));
    }

    private int n(Integer v) {
        return v == null ? 0 : v;
    }

    private String nvl(String s, String d) {
        return s == null || s.isBlank() ? d : s;
    }

    private String rating(int s) {
        return s >= 90 ? "Excellent" : s >= 75 ? "Good" : s >= 60 ? "Average" : s >= 40 ? "Needs Improvement" : "Poor";
    }

    private List<String> split(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("[,\\n;|\\u2022]"))
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .distinct()
                .toList();
    }
}
