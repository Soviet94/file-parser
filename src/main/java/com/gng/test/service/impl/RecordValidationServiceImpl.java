package com.gng.test.service.impl;

import com.gng.test.dto.FileParseErrorDTO;
import com.gng.test.model.ParsedLineResult;
import com.gng.test.model.PersonRecord;
import com.gng.test.service.RecordValidationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RecordValidationServiceImpl implements RecordValidationService {

    private final Validator validator;

    public RecordValidationServiceImpl(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates a list of parsed records.
     * @param parsedLines list of ParsedLineResult objects
     * @return list of FileParseErrorDTO representing validation errors
     */
    @Override
    public List<FileParseErrorDTO> validateRecords(List<ParsedLineResult> parsedLines) {
        List<FileParseErrorDTO> validationErrors = new ArrayList<>();

        for (ParsedLineResult lineResult : parsedLines) {
            PersonRecord record = lineResult.getRecord();

            //Skip lines that already have parse errors
            if (record == null) continue;

            Set<ConstraintViolation<PersonRecord>> violations = validator.validate(record);
            for (ConstraintViolation<PersonRecord> violation : violations) {
                String message = violation.getPropertyPath() + ": " + violation.getMessage();
                validationErrors.add(new FileParseErrorDTO(lineResult.getLineNumber(), lineResult.getRawLine(), message));
            }
        }

        return validationErrors;
    }
}