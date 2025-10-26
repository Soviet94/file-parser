package com.gng.test.model;

import com.gng.test.dto.FileParseErrorDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class ParseResult {
    private List<PersonRecord> validRecords;
    private List<FileParseErrorDTO> errors;
}