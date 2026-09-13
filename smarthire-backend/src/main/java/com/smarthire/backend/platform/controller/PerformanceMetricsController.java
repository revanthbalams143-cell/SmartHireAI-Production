package com.smarthire.backend.platform.controller;

import com.smarthire.backend.interview.entity.Interview;
import com.smarthire.backend.interview.entity.InterviewEvaluation;
import com.smarthire.backend.interview.repository.InterviewEvaluationRepository;
import com.smarthire.backend.interview.repository.InterviewRepository;
import com.smarthire.backend.platform.ai.AiUsageMetricsService;
import com.smarthire.backend.platform.dto.PerformanceMetricsResponse;
import com.smarthire.backend.repository.ResumeRepository;
import com.smarthire.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class PerformanceMetricsController {
    private final UserRepository users; private final ResumeRepository resumes; private final InterviewRepository interviews; private final InterviewEvaluationRepository evaluations; private final AiUsageMetricsService aiUsage;
    public PerformanceMetricsController(UserRepository users, ResumeRepository resumes, InterviewRepository interviews, InterviewEvaluationRepository evaluations, AiUsageMetricsService aiUsage){this.users=users;this.resumes=resumes;this.interviews=interviews;this.evaluations=evaluations;this.aiUsage=aiUsage;}
    @GetMapping("/api/analytics/performance")
    public ResponseEntity<PerformanceMetricsResponse> performance(Authentication auth){
        if(!hasRole(auth,"ROLE_ADMIN") && !hasRole(auth,"ROLE_RECRUITER")) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        List<Interview> sessions=interviews.findAll();
        List<InterviewEvaluation> scored=sessions.stream().map(i->evaluations.findByInterviewId(i.getId()).orElse(null)).filter(x->x!=null).toList();
        PerformanceMetricsResponse r=new PerformanceMetricsResponse();
        r.setTotalUsers(users.count()); r.setTotalResumes(resumes.count()); r.setTotalInterviews(sessions.size()); r.setEvaluatedInterviews(scored.size());
        r.setEvaluationCoveragePercent(sessions.isEmpty()?0:round(scored.size()*100.0/sessions.size()));
        long aiCalls=aiUsage.last24Hours(); r.setAiCallsLast24Hours(aiCalls); r.setAiSuccessRateLast24Hours(aiCalls==0?100:round(aiUsage.successfulLast24Hours()*100.0/aiCalls));
        r.setAverageOverallScore(avg(scored,0)); r.setAverageCommunicationScore(avg(scored,1)); r.setAverageConfidenceScore(avg(scored,2)); r.setAverageTechnicalScore(avg(scored,3)); r.setAverageProfessionalismScore(avg(scored,4)); r.setAveragePronunciationScore(avg(scored,5));
        Map<String,String> b=new LinkedHashMap<>();
        b.put("speechTranscriptionAccuracy","Runtime confidence only; no labeled-dataset accuracy claim.");
        b.put("emotionRecognitionAccuracy","Runtime provider confidence only; no labeled benchmark claim.");
        b.put("eyeContactAccuracy","Runtime tracking proxy only; no labeled benchmark claim.");
        b.put("communicationScoringAccuracy","Deterministic Module 7 rubric; no human-labeled benchmark claim.");
        b.put("technicalRelevance","Gemini evaluation plus deterministic fallback stored per interview.");
        b.put("concurrency","Stateless database-backed API; load test required before production.");
        b.put("apiHealth","Use /api/health and admin system health for runtime state.");
        r.setBenchmarkStatus(b); return ResponseEntity.ok(r);
    }
    private boolean hasRole(Authentication auth,String role){return auth!=null&&auth.isAuthenticated()&&auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(role::equals);}
    private double avg(List<InterviewEvaluation> list,int mode){ if(list.isEmpty()) return 0; return round(list.stream().mapToInt(x -> switch(mode){case 0->x.getOverallScore();case 1->n(x.getCommunicationScore());case 2->n(x.getConfidenceScore());case 3->x.getTechnicalScore();case 4->n(x.getProfessionalismScore());default->n(x.getPronunciationScore());}).average().orElse(0)); }
    private int n(Integer v){return v==null?0:v;} private double round(double v){return Math.round(v*10.0)/10.0;}
}
