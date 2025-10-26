package com.gng.test.service;

import com.gng.test.model.ParseResult;
import org.springframework.web.multipart.MultipartFile;

public interface FileValidationService {
    ParseResult parseAndValidate(MultipartFile file, boolean validate) throws Exception;
}
