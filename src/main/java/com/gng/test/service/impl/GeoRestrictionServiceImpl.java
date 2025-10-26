package com.gng.test.service.impl;

import com.gng.test.model.GeoInfo;
import com.gng.test.service.GeoRestrictionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;
import java.util.Set;

@Service
public class GeoRestrictionServiceImpl implements GeoRestrictionService {

    @Value("${geo.blocked.countries:CN,ES,US}")
    private Set<String> blockedCountries;

    @Value("${geo.blocked.isps:Amazon,Microsoft,Google}")
    private Set<String> blockedIsps;

    private final RestTemplate restTemplate;

    @Value("${ip-api.base.url:http://ip-api.com/json/}")
    private String ipApiBaseUrl;

    public GeoRestrictionServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void checkAccess(HttpServletRequest request) throws Exception {
        String ip = getClientIp(request);

        GeoInfo geoInfo = restTemplate.getForObject(
                ipApiBaseUrl + ip + "?fields=514",
                GeoInfo.class
        );

        if (geoInfo == null) return;

        if (blockedCountries.contains(geoInfo.getCountryCode())) {
            throw new AccessDeniedException("Access denied from country: " + geoInfo.getCountryCode());
        }

        for (String blockedIsp : blockedIsps) {
            if (geoInfo.getIsp() != null && geoInfo.getIsp().toLowerCase().contains(blockedIsp.toLowerCase())) {
                throw new AccessDeniedException("Access denied from ISP: " + geoInfo.getIsp());
            }
        }
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
