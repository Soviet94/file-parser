package com.gng.test.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gng.test.model.ApiCallLog;
import com.gng.test.model.GeoInfo;
import com.gng.test.repository.ApiCallLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;

@Component
public class ApiCallLoggingInterceptor implements HandlerInterceptor {

    private final ApiCallLogRepository apiCallLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // ThreadLocal to store request start time per request thread
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    public ApiCallLoggingInterceptor(ApiCallLogRepository apiCallLogRepository) {
        this.apiCallLogRepository = apiCallLogRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        startTime.set(System.currentTimeMillis());
        return true; // continue processing
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws UnknownHostException {
        long duration = System.currentTimeMillis() - startTime.get();
        startTime.remove();

        String clientIp = getClientIp(request);
        String country = null;
        String isp = null;

        //Lookup GeoInfo
        try {
            GeoInfo geoInfo = restTemplate.getForObject(
                    "http://ip-api.com/json/" + clientIp + "?fields=514",
                    GeoInfo.class
            );
            if (geoInfo != null) {
                country = geoInfo.getCountryCode();
                isp = geoInfo.getIsp();
            }
        } catch (Exception ignored) {
        }

        //Create log entry
        ApiCallLog log = new ApiCallLog();
        log.setRequestUri(request.getRequestURI());
        log.setRequestTimestamp(Instant.now());
        log.setIpAddress(clientIp);
        log.setCountryCode(country);
        log.setIsp(isp);
        log.setDurationMs(duration);

        //Capture HTTP status and response summary
        try {
            log.setHttpResponse("Status: " + response.getStatus());
        } catch (Exception e) {
            log.setHttpResponse("Unknown response");
        }

        apiCallLogRepository.save(log);
    }

    private String getClientIp(HttpServletRequest request) throws UnknownHostException {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getHeader("X-Real-IP");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        }

        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        if (ip.startsWith("::ffff:")) {
            ip = ip.substring(7);
        }

        InetAddress inet = InetAddress.getByName(ip);
        return inet.getHostAddress();
    }
}
