package com.workbee.app.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ConfettiView — Custom lightweight Canvas-based golden confetti emitter
 * that triggers when services are completed or payouts are processed.
 */
public class ConfettiView extends View {

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean isAnimating = false;

    // Gold colorways palette
    private final int[] goldPalette = {
            Color.parseColor("#FFD700"), // Metallic Gold
            Color.parseColor("#FFC107"), // Amber Gold
            Color.parseColor("#FFA000"), // Dark Honey Amber
            Color.parseColor("#FFE082"), // Light Translucent Gold
            Color.parseColor("#FFB300")  // Honey Gold
    };

    public ConfettiView(Context context) {
        super(context);
        init();
    }

    public ConfettiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);
    }

    /**
     * Spawns 100 cascading golden confetti particles from the top of the view.
     */
    public void startConfetti() {
        particles.clear();
        int width = getWidth();
        if (width <= 0) width = 1080; // Fallback during layout pass

        // Spawn 90 particles along the top header
        for (int i = 0; i < 90; i++) {
            float rx = random.nextFloat() * width;
            float ry = -random.nextFloat() * 400f; // Start staggered above screen
            float rvy = 6f + random.nextFloat() * 12f; // Fall speed
            float rvx = -3f + random.nextFloat() * 6f;  // Drift speed
            float size = 12f + random.nextFloat() * 20f;
            int color = goldPalette[random.nextInt(goldPalette.length)];
            float rot = random.nextFloat() * 360f;
            float rotSpeed = -5f + random.nextFloat() * 10f;
            int shape = random.nextInt(3); // 0=Rect, 1=Circle, 2=Hexagon

            particles.add(new Particle(rx, ry, rvx, rvy, size, color, rot, rotSpeed, shape));
        }

        isAnimating = true;
        invalidate();
    }

    public void stopConfetti() {
        isAnimating = false;
        particles.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isAnimating || particles.isEmpty()) return;

        boolean activeParticlesLeft = false;

        for (Particle p : particles) {
            // Update kinematics
            p.y += p.vy;
            p.x += p.vx;
            p.rotation += p.rotationSpeed;

            // Simple drag/friction
            p.vx *= 0.98f;

            // Bounce off edges slightly or stay inside bounds
            if (p.x < 0 || p.x > getWidth()) {
                p.vx = -p.vx;
            }

            // If a particle is still within the visible screen height
            if (p.y < getHeight()) {
                activeParticlesLeft = true;

                // Draw particle with customized matrix rotation
                canvas.save();
                canvas.translate(p.x, p.y);
                canvas.rotate(p.rotation);

                paint.setColor(p.color);

                // Staggered alpha transparency to make it shimmer
                paint.setAlpha(180 + (int)(random.nextFloat() * 75f));

                if (p.shape == 0) {
                    // Rectangular ribbon
                    canvas.drawRect(-p.size / 2, -p.size / 4, p.size / 2, p.size / 4, paint);
                } else if (p.shape == 1) {
                    // Circular drop
                    canvas.drawCircle(0f, 0f, p.size / 2, paint);
                } else {
                    // Hexagonal Honeycomb shard
                    float half = p.size / 2;
                    float qtr = p.size / 4;
                    android.graphics.Path hexPath = new android.graphics.Path();
                    hexPath.moveTo(0f, -half);
                    hexPath.lineTo(half, -qtr);
                    hexPath.lineTo(half, qtr);
                    hexPath.moveTo(0f, -half);
                    hexPath.lineTo(-half, -qtr);
                    hexPath.lineTo(-half, qtr);
                    hexPath.lineTo(0f, half);
                    hexPath.lineTo(half, qtr);
                    canvas.drawPath(hexPath, paint);
                }

                canvas.restore();
            }
        }

        // Loop animation if particles are still visible, otherwise end silently
        if (activeParticlesLeft) {
            invalidate();
        } else {
            isAnimating = false;
        }
    }

    private static class Particle {
        float x, y;
        float vx, vy;
        float size;
        int color;
        float rotation;
        float rotationSpeed;
        int shape;

        Particle(float x, float y, float vx, float vy, float size, int color, float rotation, float rotationSpeed, int shape) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
            this.shape = shape;
        }
    }
}
