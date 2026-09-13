package com.smarthire.backend.platform.analytics;

import com.smarthire.backend.entity.User;
import com.smarthire.backend.platform.analytics.dto.CandidateAnalyticsResponse;
import com.smarthire.backend.platform.analytics.dto.RecruiterAnalyticsResponse;
import com.smarthire.backend.platform.analytics.service.AnalyticsService;
import com.smarthire.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final UserRepository users;
    public AnalyticsController(AnalyticsService analyticsService, UserRepository users){this.analyticsService=analyticsService;this.users=users;}

    @GetMapping("/candidate/{userId}")
    public ResponseEntity<CandidateAnalyticsResponse> candidate(@PathVariable Long userId, Authentication auth){
        if(!canAccessCandidate(userId,auth)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(analyticsService.candidate(userId));
    }

    @GetMapping("/recruiter")
    public ResponseEntity<RecruiterAnalyticsResponse> recruiter(Authentication auth){
        if(!hasRole(auth,"ROLE_RECRUITER") && !hasRole(auth,"ROLE_ADMIN")) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(analyticsService.recruiter());
    }

    private boolean canAccessCandidate(Long id, Authentication auth){
        if(auth==null||!auth.isAuthenticated())return false;
        if(hasRole(auth,"ROLE_ADMIN"))return true;
        if(hasRole(auth,"ROLE_RECRUITER"))return true;
        return users.findByEmail(auth.getName()).map(User::getId).map(id::equals).orElse(false);
    }
    private boolean hasRole(Authentication auth,String role){return auth!=null&&auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(role::equals);}
}
