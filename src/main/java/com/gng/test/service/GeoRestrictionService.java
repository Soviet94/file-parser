package com.gng.test.service;

import jakarta.servlet.http.HttpServletRequest;

public interface GeoRestrictionService {
    void checkAccess(HttpServletRequest request) throws Exception;
}