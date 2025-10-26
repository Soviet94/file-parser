package com.gng.test.service.impl;

import com.gng.test.dto.FileParseErrorDTO;
import com.gng.test.model.ParsedLineResult;
import com.gng.test.model.PersonRecord;
import com.gng.test.model.ParseResult;
import com.gng.test.service.FileParsingService;
import com.gng.test.service.FileValidationService;
import com.gng.test.service.RecordValidationService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileValidationServiceImpl implements FileValidationService {

    private final FileParsingService parsingService;
    private final RecordValidationService validationService;

    public FileValidationServiceImpl(FileParsingService parsingService,
                                 RecordValidationService validationService) {
        this.parsingService = parsingService;
        this.validationService = validationService;
    }

    /**
     * Coordinates file parsing and optional validation.
     * @param file The uploaded file
     * @param validate Whether validation should be applied (flag=true)
     * @return ParseResult containing valid records and any errors
     */
    @Override
    public ParseResult parseAndValidate(MultipartFile file, boolean validate) throws Exception {

        //Parse the file (basic structure validation and conversion to records)
        List<ParsedLineResult> parsedLines = parsingService.parseFile(file);

        //Collect parsing-level errors with line content
        List<FileParseErrorDTO> parseErrors = parsedLines.stream()
                .filter(p -> p.getParseError() != null)
                .map(p -> new FileParseErrorDTO(
                        p.getLineNumber(),
                        p.getRawLine(),       // <-- include raw line content
                        p.getParseError()     // error message
                ))
                .collect(Collectors.toList());

        //Collect successfully parsed records
        List<PersonRecord> parsedRecords = parsedLines.stream()
                .map(ParsedLineResult::getRecord)
                .filter(r -> r != null)
                .collect(Collectors.toList());

        //If validation is OFF, just return parsed results
        if (!validate) {
            return new ParseResult(parsedRecords, parseErrors);
        }

        //If validation is ON, run record-level validation
        List<FileParseErrorDTO> validationErrors = validationService.validateRecords(parsedLines);

        //Merge parsing + validation errors
        parseErrors.addAll(validationErrors);

        //Determine which records are valid (no parse or validation errors)
        List<Integer> invalidLineNumbers = parseErrors.stream()
                .map(FileParseErrorDTO::getLineNumber)
                .distinct()
                .toList();

        List<PersonRecord> validRecords = parsedLines.stream()
                .filter(p -> p.getParseError() == null && !invalidLineNumbers.contains(p.getLineNumber()))
                .map(ParsedLineResult::getRecord)
                .collect(Collectors.toList());

        //Return the combined result
        return new ParseResult(validRecords, parseErrors);
    }
}