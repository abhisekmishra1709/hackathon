package com.sentinel.aml.casework;

import jakarta.validation.constraints.NotBlank;

public record CaseUpdateRequest(@NotBlank String status, String notes, String disposition) {
}