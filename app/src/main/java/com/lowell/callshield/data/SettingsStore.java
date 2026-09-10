package com.lowell.callshield.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.lowell.callshield.domain.RuleSettings;
import com.lowell.callshield.domain.ScreeningAction;
import com.lowell.callshield.domain.CallStatistics;

import java.util.HashSet;
import java.util.Collections;
import java.util.Set;

public final class SettingsStore {
    private static final String FILE_NAME = "call_shield_settings";
    private static final String BLOCK_FAILED = "block_failed_verification";
    private static final String BLOCK_FOREIGN = "block_foreign_numbers";
    private static final String BLOCK_NON_CONTACTS = "block_non_contacts";
    private static final String MATCH_ACTION = "matched_call_action";
    private static final String HOME_CALLING_CODE = "home_calling_code";
    private static final String RECEIVED_TOTAL = "received_total";
    private static final String BLOCKED_TOTAL = "blocked_total";
    private static final String BLOCKED_TIMESTAMPS = "blocked_timestamps";
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private final SharedPreferences preferences;

    public SettingsStore(Context context) {
        preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public RuleSettings getRuleSettings() {
        ScreeningAction action;
        try {
            action = ScreeningAction.valueOf(
                    preferences.getString(MATCH_ACTION, ScreeningAction.REJECT.name()));
        } catch (IllegalArgumentException exception) {
            action = ScreeningAction.REJECT;
        }
        return new RuleSettings(
                preferences.getBoolean(BLOCK_FAILED, true),
                preferences.getBoolean(BLOCK_FOREIGN, false),
                preferences.getBoolean(BLOCK_NON_CONTACTS, false),
                action);
    }

    public void setBlockFailedVerification(boolean enabled) {
        preferences.edit().putBoolean(BLOCK_FAILED, enabled).apply();
    }

    public void setBlockForeignNumbers(boolean enabled) {
        preferences.edit().putBoolean(BLOCK_FOREIGN, enabled).apply();
    }

    public void setBlockNonContacts(boolean enabled) {
        preferences.edit().putBoolean(BLOCK_NON_CONTACTS, enabled).apply();
    }

    public void setMatchedCallAction(ScreeningAction action) {
        preferences.edit().putString(MATCH_ACTION, action.name()).apply();
    }

    public String getHomeCallingCode() {
        return preferences.getString(HOME_CALLING_CODE, "+1");
    }

    public void setHomeCallingCode(String callingCode) {
        preferences.edit().putString(HOME_CALLING_CODE, normalizeCode(callingCode)).apply();
    }

    /** Records one screenable incoming call. A blocked call is one that was rejected. */
    public void recordIncomingCall(ScreeningAction action) {
        long now = System.currentTimeMillis();
        Set<String> timestamps = currentBlockedTimestamps(now);
        SharedPreferences.Editor editor = preferences.edit()
                .putLong(RECEIVED_TOTAL, preferences.getLong(RECEIVED_TOTAL, 0) + 1);
        if (action == ScreeningAction.REJECT) {
            timestamps.add(now + ":" + System.nanoTime());
            editor.putInt(BLOCKED_TOTAL, preferences.getInt(BLOCKED_TOTAL, 0) + 1);
        }
        editor.putStringSet(BLOCKED_TIMESTAMPS, timestamps).commit();
    }

    public CallStatistics getCallStatistics() {
        long now = System.currentTimeMillis();
        Set<String> timestamps = currentBlockedTimestamps(now);
        int lastDay = 0;
        int last7Days = 0;
        int last30Days = 0;
        for (String timestamp : timestamps) {
            long age = now - timestampMillis(timestamp);
            if (age <= DAY_MILLIS) lastDay++;
            if (age <= 7 * DAY_MILLIS) last7Days++;
            if (age <= 30 * DAY_MILLIS) last30Days++;
        }
        preferences.edit().putStringSet(BLOCKED_TIMESTAMPS, timestamps).apply();
        return new CallStatistics(
                preferences.getLong(RECEIVED_TOTAL, 0),
                preferences.getInt(BLOCKED_TOTAL, 0),
                lastDay,
                last7Days,
                last30Days);
    }

    private Set<String> currentBlockedTimestamps(long now) {
        Set<String> retained = new HashSet<>();
        Set<String> stored = preferences.getStringSet(BLOCKED_TIMESTAMPS, Collections.emptySet());
        for (String value : stored) {
            try {
                long timestamp = timestampMillis(value);
                if (timestamp <= now && now - timestamp <= 30 * DAY_MILLIS) retained.add(value);
            } catch (NumberFormatException ignored) {
                // Ignore corrupt values rather than interrupting call screening.
            }
        }
        return retained;
    }

    private long timestampMillis(String value) {
        int separator = value.indexOf(':');
        return Long.parseLong(separator < 0 ? value : value.substring(0, separator));
    }

    private String normalizeCode(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? "+1" : "+" + digits;
    }
}
