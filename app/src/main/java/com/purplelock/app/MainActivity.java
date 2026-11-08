package com.purplelock.app;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.appcompat.app.AppCompatActivity;

import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "purplelock_security";
    private static final String SALT_KEY = "pin_salt";
    private static final String HASH_KEY = "pin_hash";
    private static final String FAIL_KEY = "failed_attempts";
    private static final String LOCK_KEY = "lockout_until";

    private static final int PURPLE = Color.rgb(168, 85, 247);
    private static final int BACKGROUND = Color.rgb(8, 5, 16);
    private static final int CARD = Color.rgb(25, 17, 38);

    private SharedPreferences prefs;
    private LinearLayout root;
    private TextView message;
    private TextView dots;

    private String enteredPin = "";
    private String firstPin = "";
    private String screenMode = "LOCK";

    private boolean authenticated = false;
    private boolean wasBackgrounded = false;

    private CountDownTimer lockTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        if (isPinConfigured()) {
            showLockScreen();
        } else {
            showSetupScreen();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (wasBackgrounded) {
            wasBackgrounded = false;
            authenticated = false;
            if (isPinConfigured()) showLockScreen();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authenticated && !isChangingConfigurations()) {
            authenticated = false;
            wasBackgrounded = true;
        }
    }

    private boolean isPinConfigured() {
        return prefs.contains(HASH_KEY) && prefs.contains(SALT_KEY);
    }

    private void showSetupScreen() {
        screenMode = "SETUP";
        enteredPin = "";
        firstPin = "";
        buildPinScreen("CREATE YOUR PIN",
                "Choose a private six-digit PIN", "SET PIN");
    }

    private void showConfirmScreen() {
        screenMode = "CONFIRM";
        enteredPin = "";
        buildPinScreen("CONFIRM YOUR PIN",
                "Enter the same six digits again", "CONFIRM PIN");
    }

    private void showLockScreen() {
        screenMode = "LOCK";
        enteredPin = "";
        buildPinScreen("PURPLELOCK",
                "Enter your six-digit PIN", "UNLOCK");
        checkLockout();
    }

    private void buildPinScreen(
            String heading, String subtitle, String actionLabel) {

        if (lockTimer != null) {
            lockTimer.cancel();
            lockTimer = null;
        }

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(25), dp(25), dp(25), dp(25));
        root.setBackgroundColor(BACKGROUND);
        setContentView(root);

        TextView logo = new TextView(this);
        logo.setText("PURPLELOCK");
        logo.setTextColor(PURPLE);
        logo.setTextSize(29);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);

        TextView title = new TextView(this);
        title.setText(heading);
        title.setTextColor(Color.WHITE);
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(35);
        root.addView(title, titleParams);

        TextView description = new TextView(this);
        description.setText(subtitle);
        description.setTextColor(Color.LTGRAY);
        description.setTextSize(14);
        description.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams descParams =
                new LinearLayout.LayoutParams(-1, -2);
        descParams.topMargin = dp(10);
        root.addView(description, descParams);

        dots = new TextView(this);
        dots.setTextColor(PURPLE);
        dots.setTextSize(24);
        dots.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams dotsParams =
                new LinearLayout.LayoutParams(-1, -2);
        dotsParams.topMargin = dp(28);
        dotsParams.bottomMargin = dp(25);
        root.addView(dots, dotsParams);

        message = new TextView(this);
        message.setTextColor(PURPLE);
        message.setTextSize(13);
        message.setGravity(Gravity.CENTER);
        root.addView(message,
                new LinearLayout.LayoutParams(-1, dp(35)));

        GridLayout keypad = new GridLayout(this);
        keypad.setColumnCount(3);
        keypad.setRowCount(4);

        String[] keys = {
                "1", "2", "3", "4", "5", "6",
                "7", "8", "9", "CLEAR", "0", "⌫"
        };

        for (String key : keys) {
            Button button = new Button(this);
            button.setText(key);
            button.setTextColor(Color.WHITE);
            button.setTextSize(key.length() > 1 ? 12 : 22);
            button.setAllCaps(false);
            button.setBackground(makeBackground(CARD, 18, PURPLE));

            GridLayout.LayoutParams params =
                    new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(65);
            params.columnSpec =
                    GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dp(5), dp(5), dp(5), dp(5));

            keypad.addView(button, params);
            button.setOnClickListener(v -> handleKey(key));
        }

        LinearLayout.LayoutParams keypadParams =
                new LinearLayout.LayoutParams(-1, -2);
        keypadParams.topMargin = dp(5);
        root.addView(keypad, keypadParams);

        Button action = new Button(this);
        action.setText(actionLabel);
        action.setTextColor(Color.WHITE);
        action.setTextSize(15);
        action.setTypeface(Typeface.DEFAULT_BOLD);
        action.setBackground(makeBackground(PURPLE, 18, PURPLE));

        LinearLayout.LayoutParams actionParams =
                new LinearLayout.LayoutParams(-1, dp(55));
        actionParams.topMargin = dp(18);
        root.addView(action, actionParams);
        action.setOnClickListener(v -> submitPin());

        updateDots();
    }

    private void handleKey(String key) {
        if (isLockedOut()) {
            checkLockout();
            return;
        }

        if (key.equals("CLEAR")) {
            enteredPin = "";
        } else if (key.equals("⌫")) {
            if (!enteredPin.isEmpty()) {
                enteredPin =
                        enteredPin.substring(0, enteredPin.length() - 1);
            }
        } else if (enteredPin.length() < 6) {
            enteredPin += key;
        }

        updateDots();
    }

    private void updateDots() {
        if (dots == null) return;

        StringBuilder display = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            display.append(i < enteredPin.length() ? "●" : "○");
            if (i < 5) display.append("  ");
        }
        dots.setText(display.toString());
    }

    private void submitPin() {
        if (isLockedOut()) {
            checkLockout();
            return;
        }

        if (enteredPin.length() != 6) {
            showMessage("Enter all six digits");
            return;
        }

        if (screenMode.equals("SETUP")) {
            firstPin = enteredPin;
            showConfirmScreen();
            return;
        }

        if (screenMode.equals("CONFIRM")) {
            if (!firstPin.equals(enteredPin)) {
                firstPin = "";
                showSetupScreen();
                showMessage("PINs did not match. Try again.");
                return;
            }

            savePin(enteredPin);
            if (isPinConfigured()) {
                resetFailures();
                authenticated = true;
                showDashboard();
            } else {
                showMessage("Could not save PIN. Try again.");
            }
            return;
        }

        if (verifyPin(enteredPin)) {
            resetFailures();
            authenticated = true;
            showDashboard();
        } else {
            registerFailure();
            enteredPin = "";
            updateDots();

            if (isLockedOut()) {
                checkLockout();
            } else {
                showMessage("Incorrect PIN. Try again.");
            }
        }
    }

    private void savePin(String pin) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = deriveHash(pin, salt);

            prefs.edit()
                    .putString(SALT_KEY,
                            Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString(HASH_KEY,
                            Base64.encodeToString(hash, Base64.NO_WRAP))
                    .putInt(FAIL_KEY, 0)
                    .putLong(LOCK_KEY, 0)
                    .commit();
        } catch (Exception e) {
            showMessage("Could not save PIN");
        }
    }

    private boolean verifyPin(String pin) {
        try {
            String saltText = prefs.getString(SALT_KEY, null);
            String hashText = prefs.getString(HASH_KEY, null);
            if (saltText == null || hashText == null) return false;

            byte[] salt = Base64.decode(saltText, Base64.NO_WRAP);
            byte[] savedHash = Base64.decode(hashText, Base64.NO_WRAP);
            byte[] enteredHash = deriveHash(pin, salt);

            return MessageDigest.isEqual(savedHash, enteredHash);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] deriveHash(String pin, byte[] salt)
            throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
                pin.toCharArray(), salt, 150000, 256);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(
                            "PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private void registerFailure() {
        int failures = prefs.getInt(FAIL_KEY, 0) + 1;

        if (failures >= 5) {
            prefs.edit()
                    .putInt(FAIL_KEY, 0)
                    .putLong(LOCK_KEY,
                            System.currentTimeMillis() + 30000)
                    .apply();
        } else {
            prefs.edit().putInt(FAIL_KEY, failures).apply();
        }
    }

    private void resetFailures() {
        prefs.edit()
                .putInt(FAIL_KEY, 0)
                .putLong(LOCK_KEY, 0)
                .apply();
    }

    private boolean isLockedOut() {
        return System.currentTimeMillis() <
                prefs.getLong(LOCK_KEY, 0);
    }

    private void checkLockout() {
        long remaining = prefs.getLong(LOCK_KEY, 0)
                - System.currentTimeMillis();

        if (remaining <= 0) {
            if (message != null) message.setText("");
            return;
        }

        if (lockTimer != null) lockTimer.cancel();

        lockTimer = new CountDownTimer(remaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                message.setText(
                        "Too many attempts. Wait " +
                        ((millisUntilFinished + 999) / 1000) +
                        " seconds.");
            }

            @Override
            public void onFinish() {
                prefs.edit().putLong(LOCK_KEY, 0).apply();
                message.setText("You can try again.");
            }
        }.start();
    }

    private void showMessage(String text) {
        if (message != null) message.setText(text);
    }

    private void showDashboard() {
        screenMode = "DASHBOARD";

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(25), dp(25), dp(25), dp(25));
        root.setBackgroundColor(BACKGROUND);
        setContentView(root);

        TextView logo = new TextView(this);
        logo.setText("PURPLELOCK");
        logo.setTextColor(PURPLE);
        logo.setTextSize(30);
        logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);

        TextView status = new TextView(this);
        status.setText("ACCESS GRANTED");
        status.setTextColor(Color.WHITE);
        status.setTextSize(20);
        status.setGravity(Gravity.CENTER);
        status.setTypeface(Typeface.DEFAULT_BOLD);

        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(45);
        root.addView(status, statusParams);

        TextView detail = new TextView(this);
        detail.setText("Your private space is protected.\n\n" +
                "PURPLELOCK security system active.");
        detail.setTextColor(Color.LTGRAY);
        detail.setTextSize(15);
        detail.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams detailParams =
                new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(20);
        root.addView(detail, detailParams);

        Button lock = new Button(this);
        lock.setText("LOCK PURPLELOCK");
        lock.setTextColor(Color.WHITE);
        lock.setTypeface(Typeface.DEFAULT_BOLD);
        lock.setBackground(makeBackground(PURPLE, 18, PURPLE));

        LinearLayout.LayoutParams lockParams =
                new LinearLayout.LayoutParams(-1, dp(55));
        lockParams.topMargin = dp(55);
        root.addView(lock, lockParams);

        lock.setOnClickListener(v -> {
            authenticated = false;
            showLockScreen();
        });
    }

    private GradientDrawable makeBackground(
            int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onBackPressed() {
        if (authenticated) {
            authenticated = false;
            showLockScreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (lockTimer != null) lockTimer.cancel();
        super.onDestroy();
    }
}
