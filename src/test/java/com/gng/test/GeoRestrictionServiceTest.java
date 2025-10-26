package com.gng.test;

import com.gng.test.model.GeoInfo;
import com.gng.test.service.impl.GeoRestrictionServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.file.AccessDeniedException;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeoRestrictionServiceTest {

    private GeoRestrictionServiceImpl geoRestrictionService;
    private RestTemplate restTemplate;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        request = mock(HttpServletRequest.class);
        geoRestrictionService = new GeoRestrictionServiceImpl(restTemplate);

        // manually inject @Value fields
        ReflectionTestUtils.setField(geoRestrictionService, "blockedCountries", Set.of("US", "CN", "ES"));
        ReflectionTestUtils.setField(geoRestrictionService, "blockedIsps", Set.of("Amazon", "Microsoft", "Google"));
        ReflectionTestUtils.setField(geoRestrictionService, "ipApiBaseUrl", "http://ip-api.com/json/");
    }

    @Test
    void allowsAccess_whenCountryAndIspNotBlocked() throws Exception {
        when(request.getRemoteAddr()).thenReturn("8.8.8.8");

        GeoInfo geoInfo = new GeoInfo();
        geoInfo.setCountryCode("GB");
        geoInfo.setIsp("SomeISP");

        when(restTemplate.getForObject(ArgumentMatchers.contains("8.8.8.8"), eq(GeoInfo.class)))
                .thenReturn(geoInfo);

        // Should not throw
        geoRestrictionService.checkAccess(request);
    }

    @Test
    void deniesAccess_whenCountryBlocked() throws Exception {
        when(request.getRemoteAddr()).thenReturn("8.8.8.8");

        GeoInfo geoInfo = new GeoInfo();
        geoInfo.setCountryCode("US");
        geoInfo.setIsp("SomeISP");

        when(restTemplate.getForObject(anyString(), eq(GeoInfo.class)))
                .thenReturn(geoInfo);

        assertThatThrownBy(() -> geoRestrictionService.checkAccess(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("country: US");
    }

    @Test
    void deniesAccess_whenIspBlocked() throws Exception {
        when(request.getRemoteAddr()).thenReturn("8.8.8.8");

        GeoInfo geoInfo = new GeoInfo();
        geoInfo.setCountryCode("DE");
        geoInfo.setIsp("Amazon AWS");

        when(restTemplate.getForObject(anyString(), eq(GeoInfo.class)))
                .thenReturn(geoInfo);

        assertThatThrownBy(() -> geoRestrictionService.checkAccess(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ISP: Amazon AWS");
    }

    @Test
    void doesNothing_whenGeoInfoIsNull() throws Exception {
        when(request.getRemoteAddr()).thenReturn("8.8.8.8");

        when(restTemplate.getForObject(anyString(), eq(GeoInfo.class)))
                .thenReturn(null); // simulate null API response

        // should not throw anything
        geoRestrictionService.checkAccess(request);
    }

    @Test
    void extractsIpFrom_XForwardedFor_Header() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 70.41.3.18, 150.172.238.178");
        when(request.getHeader("X-Real-IP")).thenReturn(null);

        GeoInfo geoInfo = new GeoInfo();
        geoInfo.setCountryCode("GB");
        geoInfo.setIsp("TestISP");

        when(restTemplate.getForObject(contains("203.0.113.10"), eq(GeoInfo.class))).thenReturn(geoInfo);

        geoRestrictionService.checkAccess(request);

        verify(restTemplate).getForObject(contains("203.0.113.10"), eq(GeoInfo.class));
    }

    @Test
    void normalizesIpv6LoopbackTo127001() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

        GeoInfo geoInfo = new GeoInfo();
        geoInfo.setCountryCode("GB");
        geoInfo.setIsp("TestISP");

        when(restTemplate.getForObject(contains("127.0.0.1"), eq(GeoInfo.class))).thenReturn(geoInfo);

        geoRestrictionService.checkAccess(request);

        verify(restTemplate).getForObject(contains("127.0.0.1"), eq(GeoInfo.class));
    }

    @Test
    void normalizesIpv4MappedIpv6Address() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("::ffff:192.168.1.20");

        GeoInfo geoInfo = new GeoInfo();
        geoInfo.setCountryCode("GB");
        geoInfo.setIsp("TestISP");

        when(restTemplate.getForObject(contains("192.168.1.20"), eq(GeoInfo.class))).thenReturn(geoInfo);

        geoRestrictionService.checkAccess(request);

        verify(restTemplate).getForObject(contains("192.168.1.20"), eq(GeoInfo.class));
    }
}