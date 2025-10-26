package com.gng.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileParseErrorDTO {
    private Integer lineNumber;
    private String lineContent;
    private String message;
}