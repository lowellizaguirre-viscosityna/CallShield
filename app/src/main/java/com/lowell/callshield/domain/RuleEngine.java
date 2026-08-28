package com.lowell.callshield.domain;

public final class RuleEngine {
    public ScreeningDecision evaluate(CallFacts facts, RuleSettings settings) {
        if (settings.blockFailedVerification() && facts.verificationFailed()) {
            return matched(settings, MatchReason.FAILED_VERIFICATION);
        }
        if (settings.blockForeignNumbers() && facts.foreignNumber()) {
            return matched(settings, MatchReason.FOREIGN_NUMBER);
        }
        if (settings.blockNonContacts() && facts.nonContact()) {
            return matched(settings, MatchReason.NON_CONTACT);
        }
        return ScreeningDecision.allow();
    }

    private ScreeningDecision matched(RuleSettings settings, MatchReason reason) {
        return new ScreeningDecision(settings.matchedCallAction(), reason);
    }
}
