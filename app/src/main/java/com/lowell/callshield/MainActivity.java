package com.lowell.callshield;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;

import com.lowell.callshield.data.SettingsStore;
import com.lowell.callshield.domain.RuleSettings;
import com.lowell.callshield.domain.ScreeningAction;
import com.lowell.callshield.audio.FaxTonePlayer;
import com.lowell.callshield.audio.FaxButtonService;

public final class MainActivity extends Activity {
    private static final int ROLE_REQUEST_CODE = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    private SettingsStore store;
    private TextView roleStatus;
    private Button activateButton;
    private Button faxPreviewButton;
    private Button enableFaxControlButton;
    private FaxTonePlayer faxTonePlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new SettingsStore(this);
        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateRoleStatus();
    }

    private View buildContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(24), dp(24), dp(40));
        scroll.addView(content, matchWrap());

        TextView title = text("CallShield", 32, Typeface.BOLD);
        content.addView(title);
        TextView subtitle = text("Local, explainable screening for unwanted calls", 16, Typeface.NORMAL);
        subtitle.setTextColor(Color.DKGRAY);
        content.addView(subtitle);
        content.addView(space(24));

        content.addView(sectionTitle("Protection status"));
        roleStatus = text("Checking…", 16, Typeface.BOLD);
        content.addView(roleStatus);
        activateButton = new Button(this);
        activateButton.setText("Activate call screening");
        activateButton.setOnClickListener(view -> requestScreeningRole());
        content.addView(activateButton, matchWrap());

        content.addView(space(24));
        content.addView(sectionTitle("Automatic rules"));
        RuleSettings settings = store.getRuleSettings();
        content.addView(ruleSwitch(
                "Block failed caller verification",
                "Uses the carrier STIR/SHAKEN verdict. This is a risk signal, not proof of spoofing.",
                settings.blockFailedVerification(),
                store::setBlockFailedVerification));
        content.addView(ruleSwitch(
                "Block foreign numbers",
                "Matches international numbers that do not start with your home calling code.",
                settings.blockForeignNumbers(),
                store::setBlockForeignNumbers));
        content.addView(ruleSwitch(
                "Block numbers outside contacts",
                "The screening role normally receives calls from numbers outside your contacts.",
                settings.blockNonContacts(),
                store::setBlockNonContacts));

        TextView callingCodeLabel = text("Home calling code", 15, Typeface.BOLD);
        callingCodeLabel.setPadding(0, dp(12), 0, 0);
        content.addView(callingCodeLabel);
        EditText callingCode = new EditText(this);
        callingCode.setSingleLine(true);
        callingCode.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        callingCode.setText(store.getHomeCallingCode());
        callingCode.setHint("+1");
        callingCode.setOnFocusChangeListener((view, focused) -> {
            if (!focused) {
                store.setHomeCallingCode(callingCode.getText().toString());
                callingCode.setText(store.getHomeCallingCode());
            }
        });
        content.addView(callingCode, matchWrap());

        content.addView(space(24));
        content.addView(sectionTitle("When a rule matches"));
        RadioGroup actions = new RadioGroup(this);
        RadioButton reject = radio("Reject the call", ScreeningAction.REJECT,
                settings.matchedCallAction() == ScreeningAction.REJECT);
        RadioButton silence = radio("Silence the ringer", ScreeningAction.SILENCE,
                settings.matchedCallAction() == ScreeningAction.SILENCE);
        RadioButton answerWithFax = new RadioButton(this);
        answerWithFax.setId(View.generateViewId());
        answerWithFax.setText("Answer and play fax sound for 30 seconds — unavailable on cellular calls");
        answerWithFax.setTextSize(16);
        answerWithFax.setEnabled(false);
        actions.addView(reject);
        actions.addView(silence);
        actions.addView(answerWithFax);
        actions.setOnCheckedChangeListener((group, checkedId) -> {
            View checked = group.findViewById(checkedId);
            if (checked != null && checked.getTag() instanceof ScreeningAction action) {
                store.setMatchedCallAction(action);
            }
        });
        content.addView(actions);
        content.addView(body("Android does not let a third-party screening app answer a carrier call or send generated audio to the caller. You can preview the exact 30-second fax calling tone below."));
        faxPreviewButton = new Button(this);
        faxPreviewButton.setText("Preview fax sound (30 seconds)");
        faxPreviewButton.setOnClickListener(view -> toggleFaxPreview());
        content.addView(faxPreviewButton, matchWrap());

        enableFaxControlButton = new Button(this);
        enableFaxControlButton.setText("Enable call-time fax button");
        enableFaxControlButton.setOnClickListener(view -> enableFaxNotificationControl());
        content.addView(enableFaxControlButton, matchWrap());
        content.addView(body("This adds a persistent notification. After answering, turn on speakerphone, pull down notifications, and tap Play fax (30 sec). Results vary because the sound reaches the caller acoustically through the microphone."));

        content.addView(space(24));
        content.addView(sectionTitle("Platform limitations"));
        content.addView(body("Private, hidden, unavailable, and payphone presentations are not passed to third-party screening apps, so CallShield cannot reliably filter them."));
        content.addView(body("Answering a cellular call and transmitting fax audio is not available through Android's public call-screening APIs. This version does not claim to provide it."));
        Button systemSettings = new Button(this);
        systemSettings.setText("Open caller ID & spam settings");
        systemSettings.setOnClickListener(view -> openSystemSettings());
        content.addView(systemSettings, matchWrap());

        return scroll;
    }

    @Override
    protected void onDestroy() {
        stopFaxPreview();
        super.onDestroy();
    }

    private void toggleFaxPreview() {
        if (faxTonePlayer != null) {
            stopFaxPreview();
            return;
        }
        try {
            faxTonePlayer = new FaxTonePlayer();
            faxPreviewButton.setText("Stop fax sound");
            faxTonePlayer.play(() -> {
                faxTonePlayer = null;
                if (faxPreviewButton != null) {
                    faxPreviewButton.setText("Preview fax sound (30 seconds)");
                }
            });
        } catch (RuntimeException exception) {
            stopFaxPreview();
            faxPreviewButton.setText("Fax preview unavailable on this device");
            faxPreviewButton.setEnabled(false);
        }
    }

    private void stopFaxPreview() {
        if (faxTonePlayer != null) {
            faxTonePlayer.close();
            faxTonePlayer = null;
        }
        if (faxPreviewButton != null) {
            faxPreviewButton.setText("Preview fax sound (30 seconds)");
        }
    }

    private void enableFaxNotificationControl() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
            return;
        }
        startFaxButtonService();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startFaxButtonService();
            } else {
                enableFaxControlButton.setText("Notification permission required");
            }
        }
    }

    private void startFaxButtonService() {
        Intent intent = FaxButtonService.createIntent(this, FaxButtonService.ACTION_SHOW);
        startForegroundService(intent);
        enableFaxControlButton.setText("Call-time fax button enabled");
        enableFaxControlButton.setEnabled(false);
    }

    private View ruleSwitch(String title, String description, boolean checked, BooleanConsumer onChange) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        Switch toggle = new Switch(this);
        toggle.setText(title);
        toggle.setTextSize(16);
        toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((button, enabled) -> onChange.accept(enabled));
        row.addView(toggle, matchWrap());
        row.addView(body(description));
        return row;
    }

    private RadioButton radio(String label, ScreeningAction action, boolean checked) {
        RadioButton button = new RadioButton(this);
        button.setId(View.generateViewId());
        button.setText(label);
        button.setTextSize(16);
        button.setTag(action);
        button.setChecked(checked);
        return button;
    }

    private void requestScreeningRole() {
        RoleManager manager = getSystemService(RoleManager.class);
        if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Intent request = manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
            startActivityForResult(request, ROLE_REQUEST_CODE);
        }
    }

    private void updateRoleStatus() {
        if (roleStatus == null || activateButton == null) return;
        RoleManager manager = getSystemService(RoleManager.class);
        boolean active = manager != null
                && manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                && manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        roleStatus.setText(active ? "Active — calls can be screened" : "Inactive — activation required");
        roleStatus.setTextColor(active ? Color.rgb(20, 115, 55) : Color.rgb(180, 45, 35));
        activateButton.setEnabled(!active);
        activateButton.setText(active ? "Call screening is active" : "Activate call screening");
    }

    private void openSystemSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
        } catch (RuntimeException exception) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private TextView sectionTitle(String value) {
        TextView view = text(value, 20, Typeface.BOLD);
        view.setPadding(0, 0, 0, dp(8));
        return view;
    }

    private TextView body(String value) {
        TextView view = text(value, 14, Typeface.NORMAL);
        view.setTextColor(Color.DKGRAY);
        view.setPadding(dp(4), dp(2), 0, dp(6));
        return view;
    }

    private TextView text(String value, int size, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private Space space(int height) {
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return space;
    }

    private ViewGroup.LayoutParams matchWrap() {
        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface BooleanConsumer {
        void accept(boolean value);
    }
}
