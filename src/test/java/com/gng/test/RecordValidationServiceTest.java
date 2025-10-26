package com.gng.test;

import com.gng.test.dto.FileParseErrorDTO;
import com.gng.test.model.ParsedLineResult;
import com.gng.test.model.PersonRecord;
import com.gng.test.service.impl.RecordValidationServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecordValidationServiceTest {

    private Validator validator;
    private RecordValidationServiceImpl validationService;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        validationService = new RecordValidationServiceImpl(validator);
    }

    private PersonRecord createValidRecord() {
        return new PersonRecord(
                "550e8400-e29b-41d4-a716-446655440000",
                "001",
                "Alice Smith",
                "Tea",
                "Car",
                50.0,
                120.0
        );
    }

    @Test
    void validateRecords_whenValidRecord_returnsNoErrors() {
        ParsedLineResult line = new ParsedLineResult(1, "valid line", createValidRecord(), null);

        List<FileParseErrorDTO> errors = validationService.validateRecords(List.of(line));

        assertThat(errors).isEmpty();
    }

    @Test
    void validateRecords_whenRecordViolatesConstraints_returnsErrors() {
        // invalid record — fails multiple validation rules
        PersonRecord invalidRecord = new PersonRecord(
                "invalid-uuid",
                "",
                "",
                "",
                "",
                -1.0,
                -10.0
        );

        ParsedLineResult parsedLine = new ParsedLineResult(1, "bad|line|data", invalidRecord, null);

        List<FileParseErrorDTO> errors = validationService.validateRecords(List.of(parsedLine));

        assertThat(errors).isNotEmpty();
        assertThat(errors)
                .allMatch(e -> e.getLineNumber() == 1)
                .anyMatch(e -> e.getMessage().contains("Invalid UUID format"))
                .anyMatch(e -> e.getMessage().contains("must be >= 0.0"))
                .anyMatch(e -> e.getMessage().contains("is required"));
    }

    @Test
    void validateRecords_whenRecordIsNull_skipsValidation() {
        ParsedLineResult line = new ParsedLineResult(3, "bad line", null, "Parse error");

        List<FileParseErrorDTO> errors = validationService.validateRecords(List.of(line));

        assertThat(errors).isEmpty();
    }

    @Test
    void validateRecords_multipleLines_correctlyCollectsErrors() {
        ParsedLineResult valid = new ParsedLineResult(1, "good line", createValidRecord(), null);

        PersonRecord invalid = new PersonRecord(
                "not-a-uuid", "2", "Mihai", "Tea", "Bike", 10.0, 20.0
        );

        ParsedLineResult bad = new ParsedLineResult(2, "bad line", invalid, null);

        List<FileParseErrorDTO> errors = validationService.validateRecords(List.of(valid, bad));

        assertThat(errors).isNotEmpty();
        assertThat(errors)
                .extracting(FileParseErrorDTO::getLineNumber)
                .containsExactly(2);
    }

    @Test
    void validateRecords_allInvalidFields_reportsEachConstraint() {
        PersonRecord invalid = new PersonRecord("", "", "", "", "", -5.0, -10.0);
        ParsedLineResult parsed = new ParsedLineResult(4, "invalid line", invalid, null);

        Set violations = validator.validate(invalid);
        assertThat(violations.size()).isGreaterThan(0);

        List<FileParseErrorDTO> errors = validationService.validateRecords(List.of(parsed));

        assertThat(errors.size()).isEqualTo(violations.size());
        assertThat(errors.get(0).getLineContent()).isEqualTo("invalid line");
    }
}