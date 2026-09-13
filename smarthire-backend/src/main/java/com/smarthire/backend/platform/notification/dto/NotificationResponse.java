package com.smarthire.backend.platform.notification.dto;
import java.time.LocalDateTime;
public class NotificationResponse {
 private Long id,userId; private String type,title,message,actionUrl,priority; private LocalDateTime dueAt,createdAt,readAt;
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
 public String getType(){return type;} public void setType(String v){type=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getMessage(){return message;} public void setMessage(String v){message=v;} public String getActionUrl(){return actionUrl;} public void setActionUrl(String v){actionUrl=v;}
 public String getPriority(){return priority;} public void setPriority(String v){priority=v;} public LocalDateTime getDueAt(){return dueAt;} public void setDueAt(LocalDateTime v){dueAt=v;}
 public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} public LocalDateTime getReadAt(){return readAt;} public void setReadAt(LocalDateTime v){readAt=v;}
}
