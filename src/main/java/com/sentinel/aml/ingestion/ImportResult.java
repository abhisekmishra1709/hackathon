package com.sentinel.aml.ingestion;

import java.util.List;

public record ImportResult(int processed, int created, int updated, List<String> errors) {
}