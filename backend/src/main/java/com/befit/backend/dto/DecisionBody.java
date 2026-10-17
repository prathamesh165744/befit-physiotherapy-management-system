package com.befit.backend.dto;

import jakarta.validation.constraints.Size;

public record DecisionBody(@Size(max = 500) String note) {}
