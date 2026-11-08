package com.purplelock.app;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class FaceTestActivity extends AppCompatActivity {

    private final int BLACK = Color.rgb(8, 5, 16);
    private final int PURPLE = Color.rgb(168, 85, 247);
    private final int WHITE = Color.WHITE;

    private LinearLayout layout;
    private TextView message;
    private BiometricPrompt biometricPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createAuthenticationEngine();
        showLockedScreen();
    }

    private void createAuthenticationEngine() {
        Executor executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(
            this,
            executor,
            new BiometricPrompt.AuthenticationCallback() {

                @Override
                public void onAuthenticationSucceeded(
                    BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    showPrivateDashboard();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    message.setText("FACE NOT RECOGNIZED\nTRY AGAIN");
                }

                @Override
                public void onAuthenticationError(
                    int errorCode, CharSequence errorMessage) {
                    super.onAuthenticationError(errorCode, errorMessage);
                    message.setText("AUTHENTICATION STOPPED\n" + errorMessage);
                }
            }
        );
    }

    private void setupLayout() {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(30, 35, 30, 35);
        layout.setBackgroundColor(BLACK);
        setContentView(layout);
    }

    private TextView createText(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER);
        return text;
    }

    private TextView createButton(String value) {
        TextView button = createText(value, 16, BLACK);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setPadding(30, 20, 30, 20);

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(PURPLE);
        shape.setCornerRadius(24);
        button.setBackground(shape);

        return button;
    }

    private void addSpacing(int height) {
        android.view.View space = new android.view.View(this);
        layout.addView(space, new LinearLayout.LayoutParams(1, height));
    }

    private void showLockedScreen() {
        setupLayout();

        TextView title = createText("PURPLELOCK", 30, PURPLE);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        TextView face = createText("◉", 100, PURPLE);

        message = createText(
            "YOUR PRIVATE SPACE IS LOCKED\n\n" +
            "Look at your phone to verify your identity.",
            15, WHITE
        );

        TextView unlock = createButton("UNLOCK WITH FACE");

        unlock.setOnClickListener(view -> authenticate());

        layout.addView(title);
        addSpacing(30);
        layout.addView(face);
        addSpacing(25);
        layout.addView(message);
        addSpacing(35);
        layout.addView(unlock);
    }

    private void authenticate() {
        BiometricManager manager = BiometricManager.from(this);

        int result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        );

        if (result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            message.setText(
                "NO ELIGIBLE BIOMETRIC FOUND.\n" +
                "Check Android biometric settings."
            );
            return;
        }

        if (result != BiometricManager.BIOMETRIC_SUCCESS) {
            message.setText(
                "BIOMETRIC AUTHENTICATION UNAVAILABLE.\n\n" +
                "ANDROID STATUS CODE: " + result + "\n\n" +
                "We need this code to identify the exact restriction."
            );
            return;
        }

        BiometricPrompt.PromptInfo promptInfo =
            new BiometricPrompt.PromptInfo.Builder()
                .setTitle("PURPLELOCK FACE TEST")
                .setSubtitle("Verify your identity")
                .setDescription("Authenticate to enter your private space.")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .setNegativeButtonText("CANCEL")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void showPrivateDashboard() {
        setupLayout();

        TextView title = createText("ACCESS GRANTED", 28, PURPLE);
        title.setTypeface(Typeface.DEFAULT_BOLD);

        TextView content = createText(
            "WELCOME TO PURPLELOCK\n\n" +
            "BIOMETRIC VERIFICATION SUCCESSFUL\n\n" +
            "Your private space is unlocked.",
            16, WHITE
        );

        TextView lock = createButton("LOCK PRIVATE SPACE");
        lock.setOnClickListener(view -> showLockedScreen());

        layout.addView(title);
        addSpacing(35);
        layout.addView(content);
        addSpacing(35);
        layout.addView(lock);
    }
}
