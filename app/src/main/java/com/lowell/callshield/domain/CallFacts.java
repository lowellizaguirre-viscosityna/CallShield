package com.lowell.callshield.domain;

public record CallFacts(
        boolean verificationFailed,
        boolean foreignNumber,
        boolean nonContact
) {}
