package com.lowell.callshield.domain;

public record RuleSettings(
        boolean blockFailedVerification,
        boolean blockForeignNumbers,
        boolean blockNonContacts,
        ScreeningAction matchedCallAction
) {
    public RuleSettings {
        if (matchedCallAction == null || matchedCallAction == ScreeningAction.ALLOW) {
            matchedCallAction = ScreeningAction.REJECT;
        }
    }
}
