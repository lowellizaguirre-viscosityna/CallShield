package com.lowell.callshield.screening;

import android.telecom.CallScreeningService;

import com.lowell.callshield.domain.ScreeningAction;

public final class CallResponseFactory {
    private CallResponseFactory() {}

    public static CallScreeningService.CallResponse create(ScreeningAction action) {
        CallScreeningService.CallResponse.Builder builder =
                new CallScreeningService.CallResponse.Builder()
                        .setSkipCallLog(false)
                        .setSkipNotification(false);

        switch (action) {
            case REJECT -> builder.setDisallowCall(true).setRejectCall(true);
            case SILENCE -> builder.setDisallowCall(false).setRejectCall(false).setSilenceCall(true);
            case ALLOW -> builder.setDisallowCall(false).setRejectCall(false).setSilenceCall(false);
        }
        return builder.build();
    }
}
