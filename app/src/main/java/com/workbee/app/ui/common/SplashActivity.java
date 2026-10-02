package com.workbee.app.ui.common;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.workbee.app.R;
import com.workbee.app.data.model.User;
import com.workbee.app.ui.admin.AdminMainActivity;
import com.workbee.app.ui.auth.LoginActivity;
import com.workbee.app.ui.customer.CustomerMainActivity;
import com.workbee.app.ui.provider.ProviderMainActivity;
import com.workbee.app.utils.FirebaseHelper;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    private Handler beeHandler = new Handler();
    private boolean isFinishing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView imgLogo   = findViewById(R.id.img_logo);
        TextView bee1       = findViewById(R.id.bee1);
        TextView bee2       = findViewById(R.id.bee2);
        TextView bee3       = findViewById(R.id.bee3);
        TextView bee4       = findViewById(R.id.bee4);
        TextView bee5       = findViewById(R.id.bee5);
        TextView bee6       = findViewById(R.id.bee6);
        View ringOuter      = findViewById(R.id.ring_outer);
        View ringMid        = findViewById(R.id.ring_mid);
        View dot1           = findViewById(R.id.dot1);
        View dot2           = findViewById(R.id.dot2);
        View dot3           = findViewById(R.id.dot3);
        TextView txtTagline = findViewById(R.id.txt_tagline);

        // ── 1. Logo: scale-in + infinite heartbeat pulse ──────────────────────
        ScaleAnimation scaleIn = new ScaleAnimation(0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleIn.setDuration(700);
        scaleIn.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleIn.setFillAfter(true);
        scaleIn.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override public void onAnimationEnd(Animation a) {
                // Start heartbeat pulse loop
                ScaleAnimation pulse = new ScaleAnimation(1f, 1.08f, 1f, 1.08f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                pulse.setDuration(800);
                pulse.setRepeatCount(Animation.INFINITE);
                pulse.setRepeatMode(Animation.REVERSE);
                pulse.setInterpolator(new AccelerateDecelerateInterpolator());
                imgLogo.startAnimation(pulse);
            }
        });
        imgLogo.startAnimation(scaleIn);

        // ── 2. Golden rings: expanding pulse ──────────────────────────────────
        animateRingPulse(ringOuter, 1800, 0);
        animateRingPulse(ringMid, 1800, 400);

        // ── 3. Tagline: fade up ───────────────────────────────────────────────
        AlphaAnimation taglineFade = new AlphaAnimation(0f, 1f);
        taglineFade.setDuration(900);
        taglineFade.setStartOffset(600);
        taglineFade.setFillAfter(true);
        txtTagline.startAnimation(taglineFade);

        // ── 4. Flying Bees — each bee has a unique organic flight path ────────
        // Bee 1: sweeps left→right across top
        startBeeFlying(bee1,
                -300f, 80f,  // from X, from Y (translate offsets)
                300f,  -40f, // to X, to Y
                1400, 0, true);

        // Bee 2: sweeps right→left from middle-right
        startBeeFlying(bee2,
                350f, 0f,
                -350f, 60f,
                1600, 300, true);

        // Bee 3: floats up then down (bob)
        startBeeBobbing(bee3, 1000, 200);

        // Bee 4: diagonal sweep bottom-right ↗ upward
        startBeeFlying(bee4,
                200f, 150f,
                -200f, -120f,
                1500, 500, true);

        // Bee 5: small orbit around top center
        startBeeBobbing(bee5, 900, 0);

        // Bee 6: sweeps bottom-left across
        startBeeFlying(bee6,
                -260f, 100f,
                260f, -80f,
                1800, 700, true);

        // ── 5. Progress dots: sequential pulse ───────────────────────────────
        animateDotSequence(dot1, dot2, dot3);

        // ── 6. Navigate after 2.8s ────────────────────────────────────────────
        new Handler().postDelayed(() -> {
            isFinishing = true;
            beeHandler.removeCallbacksAndMessages(null);

            String currentUid = repository.getCurrentUserId();
            if (currentUid != null) {
                repository.fetchUserProfile(currentUid, new FirebaseHelper.AuthCallback() {
                    @Override
                    public void onSuccess(User user) { redirectUser(user); }
                    @Override
                    public void onFailure(String message) { navigateToLogin(); }
                });
            } else {
                SharedPreferences prefs = getSharedPreferences("WorkBeePrefs", MODE_PRIVATE);
                boolean onboarded = prefs.getBoolean("onboarded", false);
                if (onboarded) {
                    navigateToLogin();
                } else {
                    startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
                    finish();
                }
            }
        }, 2800);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /** Animates a bee view flying from (fromX,fromY) offset to (toX,toY) offset repeatedly */
    private void startBeeFlying(View bee, float fromX, float fromY, float toX, float toY,
                                long duration, long startDelay, boolean repeat) {
        TranslateAnimation fly = new TranslateAnimation(fromX, toX, fromY, toY);
        fly.setDuration(duration);
        fly.setStartOffset(startDelay);
        fly.setInterpolator(new AccelerateDecelerateInterpolator());
        fly.setFillAfter(false);
        fly.setRepeatCount(repeat ? Animation.INFINITE : 0);
        fly.setRepeatMode(Animation.REVERSE);
        bee.startAnimation(fly);
    }

    /** Bobs bee up and down gently */
    private void startBeeBobbing(View bee, long duration, long startDelay) {
        TranslateAnimation bob = new TranslateAnimation(0f, 0f, 0f, -30f);
        bob.setDuration(duration);
        bob.setStartOffset(startDelay);
        bob.setInterpolator(new AccelerateDecelerateInterpolator());
        bob.setRepeatCount(Animation.INFINITE);
        bob.setRepeatMode(Animation.REVERSE);
        bob.setFillAfter(true);
        bee.startAnimation(bob);
    }

    /** Pulsing scale+alpha animation on ring views */
    private void animateRingPulse(View ring, long duration, long delay) {
        ScaleAnimation scale = new ScaleAnimation(0.85f, 1.2f, 0.85f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(duration);
        scale.setStartOffset(delay);
        scale.setRepeatCount(Animation.INFINITE);
        scale.setRepeatMode(Animation.REVERSE);
        scale.setInterpolator(new AccelerateDecelerateInterpolator());
        ring.startAnimation(scale);
    }

    /** Sequentially pulses the 3 loading dots like a honeycomb drip */
    private void animateDotSequence(View d1, View d2, View d3) {
        Runnable dotAnim = new Runnable() {
            int step = 0;
            @Override
            public void run() {
                if (isFinishing) return;
                View[] dots = {d1, d2, d3};
                for (int i = 0; i < dots.length; i++) {
                    ScaleAnimation sa;
                    if (i == step % 3) {
                        sa = new ScaleAnimation(1f, 1.6f, 1f, 1.6f,
                                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                        sa.setDuration(280);
                        sa.setFillAfter(true);
                        sa.setRepeatCount(1);
                        sa.setRepeatMode(Animation.REVERSE);
                        dots[i].setAlpha(1f);
                    } else {
                        dots[i].setAlpha(0.35f);
                    }
                    dots[i].startAnimation(new ScaleAnimation(1f, 1f, 1f, 1f));
                }
                // Pulse active dot
                ScaleAnimation activePulse = new ScaleAnimation(1f, 1.6f, 1f, 1.6f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                activePulse.setDuration(280);
                activePulse.setFillAfter(true);
                activePulse.setRepeatCount(1);
                activePulse.setRepeatMode(Animation.REVERSE);
                dots[step % 3].startAnimation(activePulse);
                step++;
                beeHandler.postDelayed(this, 400);
            }
        };
        beeHandler.postDelayed(dotAnim, 500);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private void redirectUser(User user) {
        Intent intent;
        if (user.getRole().equalsIgnoreCase("ADMIN")) {
            intent = new Intent(SplashActivity.this, AdminMainActivity.class);
        } else if (user.getRole().equalsIgnoreCase("PROVIDER")) {
            intent = new Intent(SplashActivity.this, ProviderMainActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, CustomerMainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        finish();
    }
}
