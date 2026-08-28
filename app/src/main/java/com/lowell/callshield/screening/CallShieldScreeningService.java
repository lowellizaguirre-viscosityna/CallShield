package com.lowell.callshield.screening;

import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.Connection;
import android.util.Log;

import com.lowell.callshield.data.SettingsStore;
import com.lowell.callshield.domain.CallFacts;
import com.lowell.callshield.domain.RuleEngine;
import com.lowell.callshield.domain.ScreeningAction;
import com.lowell.callshield.domain.ScreeningDecision;

public final class CallShieldScreeningService extends CallScreeningService {
    private static final String TAG = "CallShieldScreening";

    @Override
    public void onScreenCall(Call.Details details) {
        if (details.getCallDirection() != Call.Details.DIRECTION_INCOMING) {
            return;
        }

        ScreeningAction action = ScreeningAction.ALLOW;
        try {
            SettingsStore store = new SettingsStore(this);
            String number = details.getHandle() == null
                    ? null
                    : details.getHandle().getSchemeSpecificPart();
            CallFacts facts = new CallFacts(
                    details.getCallerNumberVerificationStatus()
                            == Connection.VERIFICATION_STATUS_FAILED,
                    PhoneNumberClassifier.isForeign(number, store.getHomeCallingCode()),
                    true);
            ScreeningDecision decision = new RuleEngine().evaluate(facts, store.getRuleSettings());
            action = decision.action();
            Log.i(TAG, "Decision=" + action + ", reason=" + decision.reason());
        } catch (RuntimeException exception) {
            Log.e(TAG, "Screening failed; allowing call", exception);
        } finally {
            respondToCall(details, CallResponseFactory.create(action));
        }
    }
}
