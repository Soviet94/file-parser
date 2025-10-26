package com.gng.test;

import com.gng.test.model.ParsedLineResult;
import com.gng.test.service.FileParsingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FileParsingServiceTest {

    @Autowired
    private FileParsingService fileParsingService;

    @Test
    void parseFile_validFile_parsesSuccessfully() throws Exception {
        // given
        String line = "550e8400-e29b-41d4-a716-446655440000|123|Alice|Coffee|Car|45.5|120.0";
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.txt", "text/plain",
                line.getBytes(StandardCharsets.UTF_8)
        );

        // when
        List<ParsedLineResult> results = fileParsingService.parseFile(file);

        // then
        assertThat(results).hasSize(1);
        ParsedLineResult result = results.get(0);
        assertThat(result.getParseError()).isNull();
        assertThat(result.getRecord()).isNotNull();
        assertThat(result.getRecord().name()).isEqualTo("Alice");
    }
}