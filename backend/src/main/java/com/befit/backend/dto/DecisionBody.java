package com.befit.backend.dto;

import jakarta.validation.constraints.Size;

/** Body of approve / reject / cancel. acknowledgeWarnings = "I have read the validation warnings" (needed to approve with warnings). */
public record DecisionBody(@Size(max = 500) String note, Boolean acknowledgeWarnings) {}
