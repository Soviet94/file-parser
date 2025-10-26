package com.gng.test.service;

import com.gng.test.dto.FileParseErrorDTO;
import com.gng.test.model.ParsedLineResult;

import java.util.List;

public interface RecordValidationService {
    List<FileParseErrorDTO> validateRecords(List<ParsedLineResult> parsedLines);
}
