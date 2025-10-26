package com.gng.test.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gng.test.model.ParseResult;
import com.gng.test.service.FileValidationService;
import com.gng.test.service.GeoRestrictionService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/process")
public class FileValidationController {

    private static final Logger logger = LoggerFactory.getLogger(FileValidationController.class);
    private final FileValidationService fileValidationService;
    private final GeoRestrictionService geoRestrictionService;

    @Autowired
    public FileValidationController(FileValidationService fileValidationService,
                                    GeoRestrictionService geoRestrictionService) {
        this.fileValidationService = fileValidationService;
        this.geoRestrictionService = geoRestrictionService;
    }

    @PostMapping
    public ResponseEntity<?> ingestFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "flag", required = false) Boolean flag,
                                        HttpServletRequest request) {
        logger.info("Processing input file");
        if (flag == null) flag = false;

        try {
            geoRestrictionService.checkAccess(request);
        } catch (AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("The file cannot be empty");
        }

        try {
            //Parsing & optional validation
            ParseResult result = fileValidationService.parseAndValidate(file, flag);

            //If there were any errors (parsing or validation)
            if (!result.getErrors().isEmpty()) {
                logger.warn("File processed with {} errors", result.getErrors().size());
                return ResponseEntity.badRequest().body(result.getErrors());
            }

            List<Map<String, Object>> list = result.getValidRecords().stream()
                    .map(r -> Map.<String, Object>of(
                            "name", r.name(),
                            "transport", r.transport(),
                            "top_speed", r.top_speed()
                    ))
                    .toList();

            //Convert to JSON bytes
            ObjectMapper mapper = new ObjectMapper();
            byte[] jsonBytes = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(list);

            //Return a JSON file for download
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=OutcomeFile.json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(jsonBytes.length)
                    .body(jsonBytes);

        } catch (Exception e) {
            logger.error("Error processing file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

}