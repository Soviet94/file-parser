package com.gng.test.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ParsedLineResult {
    private final int lineNumber;
    private final String rawLine;
    private final PersonRecord record;
    private final String parseError;
}