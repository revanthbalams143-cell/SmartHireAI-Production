package com.smarthire.backend.platform.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="user_notifications", indexes={@Index(name="idx_notification_user_created", columnList="user_id,created_at"),@Index(name="idx_notification_due", columnList="due_at")})
public class UserNotification {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(nullable=false) private String type;
    @Column(nullable=false) private String title;
    @Column(columnDefinition="TEXT") private String message;
    @Column(name="action_url") private String actionUrl;
    @Column(name="event_key",unique=true) private String eventKey;
    @Column(nullable=false) private String priority="NORMAL";
    @Column(name="due_at") private LocalDateTime dueAt;
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(name="read_at") private LocalDateTime readAt;
    @Column(name="delivered_at") private LocalDateTime deliveredAt;
    @PrePersist protected void onCreate(){if(createdAt==null)createdAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getType(){return type;} public void setType(String v){type=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public String getActionUrl(){return actionUrl;} public void setActionUrl(String v){actionUrl=v;}
    public String getEventKey(){return eventKey;} public void setEventKey(String v){eventKey=v;}
    public String getPriority(){return priority;} public void setPriority(String v){priority=v;}
    public LocalDateTime getDueAt(){return dueAt;} public void setDueAt(LocalDateTime v){dueAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getReadAt(){return readAt;} public void setReadAt(LocalDateTime v){readAt=v;}
    public LocalDateTime getDeliveredAt(){return deliveredAt;} public void setDeliveredAt(LocalDateTime v){deliveredAt=v;}
}
