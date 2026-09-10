package com.lowell.callshield.domain;

/** A snapshot of calls delivered to the call-screening service. */
public record CallStatistics(
        long receivedTotal,
        int blockedTotal,
        int blockedLastDay,
        int blockedLast7Days,
        int blockedLast30Days) {}
