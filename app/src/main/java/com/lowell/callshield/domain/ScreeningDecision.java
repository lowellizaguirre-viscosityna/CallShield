package com.lowell.callshield.domain;

public record ScreeningDecision(ScreeningAction action, MatchReason reason) {
    public static ScreeningDecision allow() {
        return new ScreeningDecision(ScreeningAction.ALLOW, MatchReason.NONE);
    }
}
