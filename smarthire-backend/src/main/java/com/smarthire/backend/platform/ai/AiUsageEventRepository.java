package com.smarthire.backend.platform.ai;
import org.springframework.data.jpa.repository.JpaRepository; import java.time.LocalDateTime;
public interface AiUsageEventRepository extends JpaRepository<AiUsageEvent,Long>{ long countByCreatedAtAfter(LocalDateTime since); long countByCreatedAtAfterAndSuccessTrue(LocalDateTime since); }
