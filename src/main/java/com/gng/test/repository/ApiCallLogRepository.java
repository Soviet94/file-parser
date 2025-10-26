package com.gng.test.repository;

import com.gng.test.model.ApiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, UUID> {
}