package com.lowell.callshield.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class RuleEngineTest {
    private final RuleEngine engine = new RuleEngine();

    @Test
    public void allowsWhenNoRuleMatches() {
        ScreeningDecision result = engine.evaluate(
                new CallFacts(false, false, false),
                settings(true, true, true, ScreeningAction.REJECT));

        assertEquals(ScreeningDecision.allow(), result);
    }

    @Test
    public void failedVerificationHasHighestPriority() {
        ScreeningDecision result = engine.evaluate(
                new CallFacts(true, true, true),
                settings(true, true, true, ScreeningAction.REJECT));

        assertEquals(MatchReason.FAILED_VERIFICATION, result.reason());
        assertEquals(ScreeningAction.REJECT, result.action());
    }

    @Test
    public void foreignNumberHasPriorityOverNonContact() {
        ScreeningDecision result = engine.evaluate(
                new CallFacts(false, true, true),
                settings(false, true, true, ScreeningAction.SILENCE));

        assertEquals(MatchReason.FOREIGN_NUMBER, result.reason());
        assertEquals(ScreeningAction.SILENCE, result.action());
    }

    @Test
    public void disabledRuleDoesNotMatch() {
        ScreeningDecision result = engine.evaluate(
                new CallFacts(true, false, false),
                settings(false, false, false, ScreeningAction.REJECT));

        assertEquals(ScreeningDecision.allow(), result);
    }

    @Test
    public void allowActionIsSanitizedToReject() {
        RuleSettings settings = settings(true, false, false, ScreeningAction.ALLOW);
        ScreeningDecision result = engine.evaluate(
                new CallFacts(true, false, false), settings);

        assertEquals(ScreeningAction.REJECT, result.action());
    }

    private RuleSettings settings(
            boolean failed,
            boolean foreign,
            boolean nonContact,
            ScreeningAction action) {
        return new RuleSettings(failed, foreign, nonContact, action);
    }
}
