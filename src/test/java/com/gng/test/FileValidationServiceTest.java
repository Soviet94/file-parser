package com.gng.test;

import com.gng.test.dto.FileParseErrorDTO;
import com.gng.test.model.ParsedLineResult;
import com.gng.test.model.ParseResult;
import com.gng.test.model.PersonRecord;
import com.gng.test.service.FileParsingService;
import com.gng.test.service.FileValidationService;
import com.gng.test.service.RecordValidationService;
import com.gng.test.service.impl.FileValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileValidationServiceTest {

    private FileParsingService parsingService;
    private RecordValidationService validationService;
    private FileValidationService fileValidationService;

    @BeforeEach
    void setUp() {
        parsingService = mock(FileParsingService.class);
        validationService = mock(RecordValidationService.class);
        fileValidationService = new FileValidationServiceImpl(parsingService, validationService);
    }

    private PersonRecord createValidPerson() {
        return new PersonRecord(
                UUID.randomUUID().toString(),
                "123",
                "Alice Smith",
                "Coffee",
                "Car",
                60.0,
                120.0
        );
    }

    @Test
    void parseAndValidate_validationOff_returnsParsedResultsOnly() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/plain", "Alice,123".getBytes());

        PersonRecord record = createValidPerson();
        ParsedLineResult parsed = new ParsedLineResult(1, "Alice,123", record, null);

        when(parsingService.parseFile(file)).thenReturn(List.of(parsed));

        ParseResult result = fileValidationService.parseAndValidate(file, false);

        verify(parsingService).parseFile(file);
        verifyNoInteractions(validationService);

        assertThat(result.getValidRecords()).containsExactly(record);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void parseAndValidate_validationOn_withParseAndValidationErrors_mergesErrors() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/plain", "Line1\nLine2".getBytes());

        PersonRecord validPerson = createValidPerson();

        ParsedLineResult parsed1 = new ParsedLineResult(1, "Line1", validPerson, null);
        ParsedLineResult parsed2 = new ParsedLineResult(2, "Line2", null, "Parse error at line 2");

        when(parsingService.parseFile(file)).thenReturn(List.of(parsed1, parsed2));

        FileParseErrorDTO validationError = new FileParseErrorDTO(1, "Line1", "Name too short");
        when(validationService.validateRecords(anyList())).thenReturn(List.of(validationError));

        ParseResult result = fileValidationService.parseAndValidate(file, true);

        verify(parsingService).parseFile(file);
        verify(validationService).validateRecords(anyList());

        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors())
                .extracting(FileParseErrorDTO::getLineNumber)
                .containsExactlyInAnyOrder(1, 2);

        // Both lines have errors
        assertThat(result.getValidRecords()).isEmpty();
    }

    @Test
    void parseAndValidate_validationOn_allRecordsValid_returnsAll() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/plain", "GoodData".getBytes());

        PersonRecord person = createValidPerson();
        ParsedLineResult parsed = new ParsedLineResult(1, "GoodData", person, null);

        when(parsingService.parseFile(file)).thenReturn(List.of(parsed));
        when(validationService.validateRecords(anyList())).thenReturn(List.of());

        ParseResult result = fileValidationService.parseAndValidate(file, true);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getValidRecords()).containsExactly(person);
    }

    @Test
    void parseAndValidate_parsingThrowsException_propagatesIt() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/plain", "bad".getBytes());

        when(parsingService.parseFile(file)).thenThrow(new RuntimeException("Parse failure"));

        assertThatThrownBy(() -> fileValidationService.parseAndValidate(file, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Parse failure");
    }
}