package com.smarthire.backend.platform.ai;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="ai_usage_events", indexes={@Index(name="idx_ai_usage_created_at", columnList="createdAt"), @Index(name="idx_ai_usage_provider_operation", columnList="provider,operation")})
public class AiUsageEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; private String provider; private String operation; private boolean success; private long latencyMs; private LocalDateTime createdAt;
 @PrePersist void onCreate(){if(createdAt==null)createdAt=LocalDateTime.now();}
 public Long getId(){return id;} public String getProvider(){return provider;} public void setProvider(String v){provider=v;} public String getOperation(){return operation;} public void setOperation(String v){operation=v;} public boolean isSuccess(){return success;} public void setSuccess(boolean v){success=v;} public long getLatencyMs(){return latencyMs;} public void setLatencyMs(long v){latencyMs=v;} public LocalDateTime getCreatedAt(){return createdAt;}
}
