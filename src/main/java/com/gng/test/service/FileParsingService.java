package com.gng.test.service;

import com.gng.test.model.ParsedLineResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileParsingService {
    List<ParsedLineResult> parseFile(MultipartFile file) throws Exception;
}
