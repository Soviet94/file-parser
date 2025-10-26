package com.gng.test.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;
import java.time.Duration;

@Primary
@Service
public class CachedGeoRestrictionService implements GeoRestrictionService {

    private final GeoRestrictionService delegate;
    Cache<String, Boolean> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    public CachedGeoRestrictionService(GeoRestrictionService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void checkAccess(HttpServletRequest request) throws Exception {
        String ip = extractClientIp(request);

        //check cache
        Boolean blocked = cache.getIfPresent(ip);
        if (blocked != null) {
            if (blocked) {
                throw new AccessDeniedException("Access denied (cached) for IP: " + ip);
            }
            return;
        }

        try {
            //delegate the actual access check
            delegate.checkAccess(request);

            //cache as allowed
            cache.put(ip, false);
        } catch (AccessDeniedException ex) {
            //cache as blocked
            cache.put(ip, true);
            throw ex;
        }
    }

    private String extractClientIp(HttpServletRequest request) throws UnknownHostException {
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