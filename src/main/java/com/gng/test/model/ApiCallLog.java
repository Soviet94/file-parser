package com.gng.test.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
public class ApiCallLog {

    @Id
    @GeneratedValue
    private UUID id;

    private String requestUri;
    private Instant requestTimestamp;

    @Lob
    private String httpResponse;

    private String ipAddress;
    private String countryCode;
    private String isp;

    private long durationMs;
}
