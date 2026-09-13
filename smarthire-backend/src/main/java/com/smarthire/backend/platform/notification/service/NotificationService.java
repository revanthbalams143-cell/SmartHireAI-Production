package com.smarthire.backend.platform.notification.service;

import com.smarthire.backend.entity.Resume;
import com.smarthire.backend.entity.User;
import com.smarthire.backend.interview.entity.Interview;
import com.smarthire.backend.interview.entity.InterviewEvaluation;
import com.smarthire.backend.interview.repository.InterviewEvaluationRepository;
import com.smarthire.backend.interview.repository.InterviewRepository;
import com.smarthire.backend.repository.ResumeRepository;
import com.smarthire.backend.repository.UserRepository;
import com.smarthire.backend.platform.notification.dto.CreateReminderRequest;
import com.smarthire.backend.platform.notification.dto.NotificationResponse;
import com.smarthire.backend.platform.notification.entity.UserNotification;
import com.smarthire.backend.platform.notification.repository.UserNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final UserNotificationRepository notifications; private final InterviewRepository interviews; private final InterviewEvaluationRepository evaluations; private final ResumeRepository resumes; private final UserRepository users;
    public NotificationService(UserNotificationRepository notifications,InterviewRepository interviews,InterviewEvaluationRepository evaluations,ResumeRepository resumes,UserRepository users){this.notifications=notifications;this.interviews=interviews;this.evaluations=evaluations;this.resumes=resumes;this.users=users;}

    @Transactional
    public List<NotificationResponse> forUser(Long userId){
        User user=users.findById(userId).orElseThrow(()->new IllegalArgumentException("User not found"));
        syncRealEvents(user);
        if("candidate".equalsIgnoreCase(user.getRole())) return notifications.findTop50VisibleByUserId(userId, LocalDateTime.now()).stream().map(this::map).toList();

        // Role-specific operational notifications. Recruiters see hiring activity;
        // admins see platform/security/AI-health activity. Neither role receives
        // another role's private workspace notifications.
        List<UserNotification> own=notifications.findTop50VisibleByUserId(userId, LocalDateTime.now());
        List<Interview> latestInterviews = interviews.findAll().stream()
                .sorted(Comparator.comparing(Interview::getCreatedAt,Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10).toList();
        if("recruiter".equalsIgnoreCase(user.getRole())) {
            for(Interview i: latestInterviews) {
                InterviewEvaluation e=evaluations.findByInterviewId(i.getId()).orElse(null);
                String key="RECRUITER:INTERVIEW:"+i.getId()+":"+(e==null?"SESSION":"EVALUATED");
                if(!notifications.existsByEventKey(key)) add(userId,e==null?"INTERVIEW":"EVALUATION",e==null?"Interview activity":"Candidate evaluation ready",e==null?"A mock interview session is available for recruiter review.":"A completed AI evaluation is ready for candidate review.","/pages/recruiter.html",key,e==null?"NORMAL":"HIGH",null);
            }
        } else if("admin".equalsIgnoreCase(user.getRole())) {
            long usersCount=users.count();
            add(userId,"USER_ACTIVITY","Platform user activity","SmartHire currently tracks "+usersCount+" registered platform users.","/pages/admin.html","ADMIN:USERS:"+usersCount,"NORMAL",null);
            if(!latestInterviews.isEmpty()) add(userId,"SYSTEM","Recent interview activity","The platform has recent interview activity requiring operational monitoring.","/pages/admin.html","ADMIN:INTERVIEWS:"+latestInterviews.get(0).getId(),"NORMAL",null);
            add(userId,"AI_HEALTH","AI monitoring status","Review AI service health, CNN monitoring and analytics from the Admin Control Center.","/pages/admin.html#system-activity","ADMIN:AI_HEALTH","HIGH",null);
        }
        if(own.size()<10 && "recruiter".equalsIgnoreCase(user.getRole())) {
            add(userId,"RECRUITER","Hiring workspace ready","Candidate ranking and interview analytics are available for recruiter review.","/pages/recruiter.html#recruiter-analytics","RECRUITER:WORKSPACE","NORMAL",null);
        }
        return notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream().map(this::map).toList();
    }

    @Transactional
    public NotificationResponse createReminder(Long userId,CreateReminderRequest request){
        if(request==null||request.getDueAt()==null) throw new IllegalArgumentException("Reminder dueAt is required");
        if(request.getDueAt().isBefore(LocalDateTime.now().minusMinutes(1))) throw new IllegalArgumentException("Reminder time must be in the future");
        UserNotification n=new UserNotification(); n.setUserId(userId); n.setType("REMINDER"); n.setTitle(blank(request.getTitle(),"Interview reminder")); n.setMessage(blank(request.getMessage(),"Your SmartHire AI practice reminder is due.")); n.setActionUrl(blank(request.getActionUrl(),"/pages/interview-setup.html")); n.setDueAt(request.getDueAt()); n.setEventKey("REMINDER:"+userId+":"+UUID.randomUUID()); n.setPriority("HIGH"); return map(notifications.save(n));
    }
    @Transactional public void markRead(Long id,Long userId){UserNotification n=notifications.findById(id).orElseThrow(()->new IllegalArgumentException("Notification not found")); if(!Objects.equals(n.getUserId(),userId))throw new SecurityException("Forbidden"); n.setReadAt(LocalDateTime.now()); notifications.save(n);}
    @Transactional public void markAllRead(Long userId){for(UserNotification n:notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId)){if(n.getReadAt()==null)n.setReadAt(LocalDateTime.now());} }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void releaseDueReminders(){
        LocalDateTime now = LocalDateTime.now();
        for(UserNotification n : notifications.findByDueAtLessThanEqualAndDeliveredAtIsNull(now)){
            n.setDeliveredAt(now);
            if(n.getTitle()==null || n.getTitle().isBlank()) n.setTitle("Interview reminder");
            notifications.save(n);
        }
    }

    private void syncRealEvents(User user){
        if("candidate".equalsIgnoreCase(user.getRole())){
            for(Resume r:resumes.findAll().stream().filter(x->Objects.equals(x.getUserId(),user.getId())).limit(10).toList()) add(user.getId(),"RESUME","Resume analysis available","Your uploaded resume has stored analysis results.","/pages/resume.html","RESUME:"+r.getId()+":"+r.getUpdatedAt(),"NORMAL",null);
            for(Interview i:interviews.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().limit(20).toList()){
                add(user.getId(),"INTERVIEW","Interview session started","Your mock interview session is now stored in Interview History.","/pages/interview-history.html","INTERVIEW:"+i.getId()+":CREATED","NORMAL",null);
                InterviewEvaluation e=evaluations.findByInterviewId(i.getId()).orElse(null);
                if(e!=null){ add(user.getId(),"SESSION_COMPLETED","Interview session completed","Your session is complete and the evaluation workflow has finished.","/pages/interview-history.html","INTERVIEW:"+i.getId()+":COMPLETED","HIGH",null); add(user.getId(),"EVALUATION","Evaluation completed","Your AI evaluation and scores are ready to review.","/pages/interview-report.html?interviewId="+i.getId(),"INTERVIEW:"+i.getId()+":EVALUATED","HIGH",null); add(user.getId(),"REPORT","Report ready","Your detailed interview report is available to view and download.","/pages/interview-report.html?interviewId="+i.getId(),"INTERVIEW:"+i.getId()+":REPORT","HIGH",null); }
                if((i.getVideoRecordingName()!=null&&!i.getVideoRecordingName().isBlank())||(i.getAudioRecordingName()!=null&&!i.getAudioRecordingName().isBlank())) add(user.getId(),"RECORDING","Recording available","Your interview recording metadata is available in the report/history view.","/pages/interview-history.html","INTERVIEW:"+i.getId()+":RECORDING","NORMAL",null);
            }
        }
    }
    private void add(Long uid,String type,String title,String msg,String url,String key,String pr,LocalDateTime due){if(notifications.existsByEventKey(key))return;UserNotification n=new UserNotification();n.setUserId(uid);n.setType(type);n.setTitle(title);n.setMessage(msg);n.setActionUrl(url);n.setEventKey(key);n.setPriority(pr);n.setDueAt(due);notifications.save(n);}
    private NotificationResponse map(UserNotification n){NotificationResponse r=new NotificationResponse();r.setId(n.getId());r.setUserId(n.getUserId());r.setType(n.getType());r.setTitle(n.getTitle());r.setMessage(n.getMessage());r.setActionUrl(n.getActionUrl());r.setPriority(n.getPriority());r.setDueAt(n.getDueAt());r.setCreatedAt(n.getCreatedAt());r.setReadAt(n.getReadAt());return r;}
    private String blank(String s,String d){return s==null||s.isBlank()?d:s.trim();}
}
