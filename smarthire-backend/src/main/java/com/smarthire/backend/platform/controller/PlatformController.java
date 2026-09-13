package com.smarthire.backend.platform.controller;

import com.smarthire.backend.ai.analytics.AnalyticsPdfService;
import com.smarthire.backend.platform.dto.PlatformDashboardResponse;
import com.smarthire.backend.platform.service.PlatformInsightsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smarthire.backend.entity.User;
import com.smarthire.backend.repository.UserRepository;
import com.smarthire.backend.interview.repository.InterviewRepository;
import com.smarthire.backend.interview.entity.Interview;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping
public class PlatformController {

    private final PlatformInsightsService platformInsightsService;
    private final AnalyticsPdfService analyticsPdfService;
    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;

    public PlatformController(PlatformInsightsService platformInsightsService,
                              AnalyticsPdfService analyticsPdfService,
                              UserRepository userRepository,
                              InterviewRepository interviewRepository) {
        this.platformInsightsService = platformInsightsService;
        this.analyticsPdfService = analyticsPdfService;
        this.userRepository = userRepository;
        this.interviewRepository = interviewRepository;
    }

    @GetMapping("/api/admin/dashboard")
    public ResponseEntity<PlatformDashboardResponse> getAdminDashboard() {
        return ResponseEntity.ok(platformInsightsService.buildDashboard());
    }

    /**
     * Public, unauthenticated version of only non-sensitive aggregate cards
     * used by the marketing/landing page. Operational health and AI telemetry
     * are intentionally excluded from the public response.
     * The landing page previously called the ADMIN-only
     * /api/admin/dashboard endpoint directly, which always returned 403 for
     * anonymous visitors. This endpoint deliberately returns just the
     * non-sensitive counters - never the user list, activity log, or
     * candidate rankings that the full admin dashboard exposes - so the real
     * /api/admin/** endpoints can stay strictly ADMIN-protected.
     */
    @GetMapping("/api/public/platform-stats")
    public ResponseEntity<PlatformDashboardResponse> getPublicPlatformStats() {
        PlatformDashboardResponse full = platformInsightsService.buildDashboard();
        PlatformDashboardResponse publicStats = new PlatformDashboardResponse();
        publicStats.setStats(full.getStats().stream()
                .filter(card -> !"System Health".equalsIgnoreCase(card.getLabel())
                        && !"AI Usage".equalsIgnoreCase(card.getLabel()))
                .collect(java.util.stream.Collectors.toList()));
        return ResponseEntity.ok(publicStats);
    }


    @GetMapping("/api/admin/logs")
    public ResponseEntity<PlatformDashboardResponse> getLogs() {
        return ResponseEntity.ok(platformInsightsService.buildDashboard());
    }

    @GetMapping("/api/admin/config")
    public ResponseEntity<PlatformDashboardResponse> getConfig() {
        return ResponseEntity.ok(platformInsightsService.buildDashboard());
    }

    @GetMapping("/api/analytics/overview")
    public ResponseEntity<PlatformDashboardResponse> getAnalyticsOverview(Authentication auth) {
        if (!isRecruiterOrAdmin(auth)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(platformInsightsService.buildDashboard());
    }

    @GetMapping("/api/analytics/leaderboard")
    public ResponseEntity<PlatformDashboardResponse> getLeaderboard(Authentication auth) {
        if (!isRecruiterOrAdmin(auth)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(platformInsightsService.buildDashboard());
    }

    @GetMapping("/api/analytics/trends")
    public ResponseEntity<PlatformDashboardResponse> getTrends(Authentication auth) {
        if (!isRecruiterOrAdmin(auth)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(platformInsightsService.buildDashboard());
    }

    @GetMapping("/api/analytics/interview/{interviewId}/report/pdf")
    public ResponseEntity<byte[]> downloadInterviewReport(@PathVariable Long interviewId, Authentication auth) {
        Interview interview = interviewRepository.findById(interviewId).orElse(null);
        if (interview == null || !canViewUser(interview.getUserId(), auth)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            byte[] pdfBytes = analyticsPdfService.generateInterviewReport(interviewId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "smarthire-interview-report-" + interviewId + ".pdf");
            headers.setContentLength(pdfBytes.length);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/analytics/report/{userId}")
    public ResponseEntity<byte[]> downloadAnalyticsReport(@PathVariable Long userId, Authentication auth) {
        if (!canViewUser(userId, auth)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            byte[] pdfBytes = analyticsPdfService.generateAnalyticsReport(userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "smarthire-analytics-report-" + userId + ".pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean isRecruiterOrAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority()) || "ROLE_RECRUITER".equals(authority.getAuthority())) return true;
        }
        return false;
    }

    private boolean canViewUser(Long userId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority()) || "ROLE_RECRUITER".equals(authority.getAuthority())) return true;
        }
        return userRepository.findByEmail(auth.getName()).map(User::getId).map(userId::equals).orElse(false);
    }
}
