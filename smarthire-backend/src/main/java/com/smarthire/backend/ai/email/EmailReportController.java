package com.smarthire.backend.ai.email;

import com.smarthire.backend.entity.User;
import com.smarthire.backend.interview.dto.InterviewReportResponse;
import com.smarthire.backend.interview.entity.Interview;
import com.smarthire.backend.interview.service.InterviewService;
import com.smarthire.backend.ai.analytics.AnalyticsPdfService;
import com.smarthire.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailReportController {
    private final EmailService emailService; private final InterviewService interviewService; private final UserRepository users; private final AnalyticsPdfService analyticsPdfService;
    public EmailReportController(EmailService emailService, InterviewService interviewService, UserRepository users, AnalyticsPdfService analyticsPdfService){this.emailService=emailService;this.interviewService=interviewService;this.users=users;this.analyticsPdfService=analyticsPdfService;}
    @PostMapping("/interview-report/{interviewId}")
    public ResponseEntity<Map<String,String>> sendInterviewReport(@PathVariable Long interviewId,@RequestBody Map<String,String> request,Authentication auth){
        Interview interview=interviewService.getInterviewById(interviewId);
        if(!canAccess(interview,auth)) return ResponseEntity.status(403).body(Map.of("message","You are not authorized to email this report."));
        String recipient=request==null?"":request.getOrDefault("recipient","");
        if(recipient==null||recipient.isBlank()) return ResponseEntity.badRequest().body(Map.of("message","Recipient email is required"));
        if(!emailService.isConfigured()) return ResponseEntity.status(503).body(Map.of("message","SMTP is not configured. Set mail.smtp.host to enable email delivery."));
        try{
            InterviewReportResponse report=interviewService.getInterviewReport(interviewId);
            String subject="SmartHire AI Interview Report - Interview #"+interviewId;
            String body="Hello,\n\nYour SmartHire AI interview report is ready.\n\nJob Role: "+report.getJobRole()+"\nOverall Score: "+(report.getEvaluation()==null?"Pending":report.getEvaluation().getOverallScore()+"%")+"\nSummary: "+report.getSessionSummary()+"\n\nPlease open SmartHire AI to view the full report, feedback and downloadable analytics.\n";
            emailService.sendEmailWithAttachment(recipient,subject,body,analyticsPdfService.generateInterviewReport(interviewId),"smarthire-interview-report-"+interviewId+".pdf");
            return ResponseEntity.ok(Map.of("message","Interview report email sent successfully"));
        }catch(Exception e){return ResponseEntity.status(500).body(Map.of("message","Failed to send email: "+e.getMessage()));}
    }
    private boolean canAccess(Interview i,Authentication auth){if(auth==null||!auth.isAuthenticated())return false;for(GrantedAuthority a:auth.getAuthorities())if("ROLE_ADMIN".equals(a.getAuthority())||"ROLE_RECRUITER".equals(a.getAuthority()))return true;return users.findByEmail(auth.getName()).map(User::getId).map(id->id.equals(i.getUserId())).orElse(false);}
}
