package com.smarthire.backend.platform.notification.controller;

import com.smarthire.backend.platform.notification.dto.CreateReminderRequest;
import com.smarthire.backend.platform.notification.dto.NotificationResponse;
import com.smarthire.backend.platform.notification.service.NotificationService;
import com.smarthire.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
 private final NotificationService service; private final UserRepository users;
 public NotificationController(NotificationService service,UserRepository users){this.service=service;this.users=users;}
 @GetMapping public ResponseEntity<List<NotificationResponse>> list(Authentication auth){Long id=currentId(auth); if(id==null)return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); return ResponseEntity.ok(service.forUser(id));}
 @PostMapping("/reminders") public ResponseEntity<NotificationResponse> reminder(@RequestBody CreateReminderRequest req,Authentication auth){Long id=currentId(auth);if(id==null)return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();return ResponseEntity.ok(service.createReminder(id,req));}
 @PostMapping("/{id}/read") public ResponseEntity<Void> read(@PathVariable Long id,Authentication auth){Long uid=currentId(auth);if(uid==null)return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();try{service.markRead(id,uid);return ResponseEntity.noContent().build();}catch(SecurityException e){return ResponseEntity.status(HttpStatus.FORBIDDEN).build();}}
 @PostMapping("/read-all") public ResponseEntity<Void> readAll(Authentication auth){Long uid=currentId(auth);if(uid==null)return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();service.markAllRead(uid);return ResponseEntity.noContent().build();}
 private Long currentId(Authentication auth){if(auth==null||!auth.isAuthenticated())return null;return users.findByEmail(auth.getName()).map(u->u.getId()).orElse(null);}
}
