package com.smarthire.backend.platform.notification.dto;
import java.time.LocalDateTime;
public class CreateReminderRequest { private String title; private String message; private String actionUrl; private LocalDateTime dueAt; public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getMessage(){return message;} public void setMessage(String v){message=v;} public String getActionUrl(){return actionUrl;} public void setActionUrl(String v){actionUrl=v;} public LocalDateTime getDueAt(){return dueAt;} public void setDueAt(LocalDateTime v){dueAt=v;} }
