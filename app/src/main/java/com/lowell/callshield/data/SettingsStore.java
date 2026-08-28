package com.lowell.callshield.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.lowell.callshield.domain.RuleSettings;
import com.lowell.callshield.domain.ScreeningAction;

public final class SettingsStore {
    private static final String FILE_NAME = "call_shield_settings";
    private static final String BLOCK_FAILED = "block_failed_verification";
    private static final String BLOCK_FOREIGN = "block_foreign_numbers";
    private static final String BLOCK_NON_CONTACTS = "block_non_contacts";
    private static final String MATCH_ACTION = "matched_call_action";
    private static final String HOME_CALLING_CODE = "home_calling_code";

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

    private String normalizeCode(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? "+1" : "+" + digits;
    }
}
