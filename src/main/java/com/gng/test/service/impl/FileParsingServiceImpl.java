package com.gng.test.service.impl;

import com.gng.test.model.ParsedLineResult;
import com.gng.test.model.PersonRecord;
import com.gng.test.service.FileParsingService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileParsingServiceImpl implements FileParsingService {

    private static final String DELIMITER = "\\|";

    /**
     * Parses a file into a list of ParsedLineResult objects.
     * @param file the uploaded MultipartFile
     * @return list of ParsedLineResult, each containing either a PersonRecord or a parseError
     * @throws Exception if file reading fails
     */
    @Override
    public List<ParsedLineResult> parseFile(MultipartFile file) throws Exception {
        List<ParsedLineResult> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.isBlank()) {
                    lineNumber++;
                    continue;
                }

                String[] parts = line.split(DELIMITER);
                if (parts.length != 7) {
                    results.add(new ParsedLineResult(lineNumber, line, null, "Incorrect number of fields, expected 7"));
                    lineNumber++;
                    continue;
                }

                try {
                    PersonRecord record = PersonRecord.builder()
                            .uuid(parts[0].trim())
                            .id(parts[1].trim())
                            .name(parts[2].trim())
                            .likes(parts[3].trim())
                            .transport(parts[4].trim())
                            .avgSpeed(Double.parseDouble(parts[5].trim()))
                            .topSpeed(Double.parseDouble(parts[6].trim()))
                            .build();

                    results.add(new ParsedLineResult(lineNumber, line, record, null));
                } catch (NumberFormatException e) {
                    results.add(new ParsedLineResult(lineNumber, line, null, "Invalid number format in avg_speed or top_speed"));
                }

                lineNumber++;
            }
        }

        return results;
    }
}