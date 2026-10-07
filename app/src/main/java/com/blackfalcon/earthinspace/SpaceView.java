package com.blackfalcon.earthinspace;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/** Draws three views of where Earth is: the solar system, the nearby stars, the Milky Way. */
public class SpaceView extends View {

    public interface Listener { void onSelect(int mode, int index); }

    public static final int SOLAR = 0, STARS = 1, GALAXY = 2;
    public static final int SUN = 8; // selection index of the Sun in the solar view

    private static final int BG = 0xFF02030A;
    private static final int GOLD = 0xFFFFC107, GREEN = 0xFF2ECC71, GRAYLINE = 0xFF3A3A60;
    private static final int[] PCOLOR = {0xFFB0B0B0, 0xFFE8CDA2, 0xFF3FA7FF, 0xFFE2563B, 0xFFD9A066, 0xFFE3C77D, 0xFF7DE3E3, 0xFF4B70DD};
    private static final float[] PSIZE = {0.016f, 0.021f, 0.024f, 0.019f, 0.034f, 0.030f, 0.024f, 0.024f};
    private static final String[] QNAME = {"Mar equinox", "Jun solstice", "Sep equinox", "Dec solstice"};

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private Listener listener;
    private int mode = SOLAR;
    private float zoom = 0.4f;
    private int selected = 2;

    // solar data (polar: theta radians, r AU)
    private final double[] pTheta = new double[8], pR = new double[8];
    private double[][] orbTheta, orbR;
    private double[] trTheta, trR;
    private final double[] qTheta = new double[4], qR = new double[4];
    private final double[] apTheta = new double[2], apR = new double[2]; // perihelion, aphelion
    private boolean haveSolar = false;

    private final float[] hitX = new float[40], hitY = new float[40];
    private int hitN = 0;

    public SpaceView(Context c) { super(c); }

    public void setListener(Listener l) { listener = l; }
    public void setMode(int m) { mode = m; selected = m == SOLAR ? 2 : -1; invalidate(); }
    public void setZoom(float z) { zoom = Math.max(0f, Math.min(1f, z)); invalidate(); }
    public void setSelected(int i) { selected = i; invalidate(); }
    public int getSelected() { return selected; }

    public static double solarRmax(float zoom) { return 2.0 * Math.pow(16.0, zoom); }   // 2 .. 32 AU
    public static double starsDmax(float zoom) { return 15.0 * Math.pow(1000.0 / 15.0, zoom); } // 15 .. 1000 ly
    public static double galaxyScale(float zoom) { return Math.pow(8.0, zoom); }         // x1 .. x8

    public void setSolar(double[][] planetsXyz, double[][] orbits, double[][] trail, double[][] quarters, double[][] periAp) {
        for (int i = 0; i < 8; i++) { pTheta[i] = Math.atan2(planetsXyz[i][1], planetsXyz[i][0]); pR[i] = Math.hypot(planetsXyz[i][0], planetsXyz[i][1]); }
        orbTheta = new double[8][]; orbR = new double[8][];
        for (int i = 0; i < 8; i++) {
            int n = orbits[i].length / 2;
            orbTheta[i] = new double[n]; orbR[i] = new double[n];
            for (int j = 0; j < n; j++) { orbTheta[i][j] = orbits[i][2 * j]; orbR[i][j] = orbits[i][2 * j + 1]; }
        }
        int n = trail[0].length;
        trTheta = new double[n]; trR = new double[n];
        for (int j = 0; j < n; j++) { trTheta[j] = Math.atan2(trail[1][j], trail[0][j]); trR[j] = Math.hypot(trail[0][j], trail[1][j]); }
        for (int q = 0; q < 4; q++) { qTheta[q] = Math.atan2(quarters[q][1], quarters[q][0]); qR[q] = Math.hypot(quarters[q][0], quarters[q][1]); }
        for (int k = 0; k < 2; k++) { apTheta[k] = Math.atan2(periAp[k][1], periAp[k][0]); apR[k] = Math.hypot(periAp[k][0], periAp[k][1]); }
        haveSolar = true;
        invalidate();
    }

    // ---------------------------------------------------------------- helpers
    private void text(Canvas c, String s, float x, float y, float size, int color, boolean bold, Paint.Align al) {
        tp.setTextAlign(al);
        tp.setTextSize(size);
        tp.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        tp.setStyle(Paint.Style.STROKE);
        tp.setStrokeWidth(size * 0.22f);
        tp.setColor(BG);
        c.drawText(s, x, y, tp);
        tp.setStyle(Paint.Style.FILL);
        tp.setColor(color);
        c.drawText(s, x, y, tp);
    }

    private void backdrop(Canvas c, float w, float h) {
        c.drawColor(BG);
        Random r = new Random(11);
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 70; i++) {
            p.setColor(Color.argb(60 + r.nextInt(120), 255, 255, 255));
            c.drawCircle(r.nextFloat() * w, r.nextFloat() * h, 0.8f + r.nextFloat() * 1.2f, p);
        }
    }

    private void ring(Canvas c, float cx, float cy, float rad, int color, float stroke) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(stroke);
        p.setColor(color);
        c.drawCircle(cx, cy, rad, p);
        p.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        hitN = 0;
        backdrop(c, w, h);
        if (mode == SOLAR) drawSolar(c, w, h);
        else if (mode == STARS) drawStars(c, w, h);
        else drawGalaxy(c, w, h);
    }

    // ------------------------------------------------------------ solar system
    private double rmaxCur = 4;
    private float rViewCur = 100;

    private float radSolar(double r) { return (float) (rViewCur * Math.log(1 + r / 0.25) / Math.log(1 + rmaxCur / 0.25)); }

    private void drawSolar(Canvas c, float w, float h) {
        float unit = Math.min(w, h), cx = w / 2f, cy = h / 2f;
        rViewCur = unit * 0.46f;
        rmaxCur = solarRmax(zoom);
        if (!haveSolar) return;

        // orbits
        for (int i = 0; i < 8; i++) {
            if (Orbits.AXIS[i] > rmaxCur * 1.03) continue;
            path.reset();
            for (int j = 0; j < orbTheta[i].length; j++) {
                float rr = radSolar(orbR[i][j]);
                float x = cx + rr * (float) Math.cos(orbTheta[i][j]), y = cy - rr * (float) Math.sin(orbTheta[i][j]);
                if (j == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            path.close();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(unit * (i == 2 ? 0.004f : 0.0026f));
            p.setColor(i == 2 ? 0xAA2ECC71 : GRAYLINE);
            c.drawPath(path, p);
        }

        // the part of its orbit Earth has already travelled this year
        if (trTheta != null && trTheta.length > 1) {
            path.reset();
            for (int j = 0; j < trTheta.length; j++) {
                float rr = radSolar(trR[j]);
                float x = cx + rr * (float) Math.cos(trTheta[j]), y = cy - rr * (float) Math.sin(trTheta[j]);
                if (j == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(unit * 0.011f);
            p.setColor(GREEN);
            c.drawPath(path, p);
        }
        p.setStyle(Paint.Style.FILL);

        // equinoxes, solstices, perihelion, aphelion
        float er = radSolar(1.0);
        for (int q = 0; q < 4; q++) {
            float rr = radSolar(qR[q]);
            float x = cx + rr * (float) Math.cos(qTheta[q]), y = cy - rr * (float) Math.sin(qTheta[q]);
            p.setColor(Color.WHITE);
            c.drawRect(x - unit * 0.007f, y - unit * 0.007f, x + unit * 0.007f, y + unit * 0.007f, p);
            if (rmaxCur < 6) {
                float ox = (float) Math.cos(qTheta[q]) * unit * 0.02f, oy = -(float) Math.sin(qTheta[q]) * unit * 0.02f;
                text(c, QNAME[q], x + ox * 1.2f, y + oy * 1.2f + unit * 0.01f, unit * 0.026f, 0xFFB8B8D0, false,
                        Math.cos(qTheta[q]) > 0.3 ? Paint.Align.LEFT : (Math.cos(qTheta[q]) < -0.3 ? Paint.Align.RIGHT : Paint.Align.CENTER));
            }
        }
        for (int k = 0; k < 2; k++) {
            float rr = radSolar(apR[k]);
            float x = cx + rr * (float) Math.cos(apTheta[k]), y = cy - rr * (float) Math.sin(apTheta[k]);
            p.setColor(0xFFFF8A65);
            c.drawCircle(x, y, unit * 0.006f, p);
            if (rmaxCur < 6) text(c, k == 0 ? "P" : "A", x + unit * 0.012f, y - unit * 0.008f, unit * 0.024f, 0xFFFF8A65, true, Paint.Align.LEFT);
        }

        // Sun
        p.setShader(new RadialGradient(cx, cy, unit * 0.06f, 0xFFFFE082, 0x00FFC107, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, unit * 0.06f, p);
        p.setShader(null);
        p.setColor(GOLD);
        c.drawCircle(cx, cy, unit * 0.026f, p);
        text(c, "Sun", cx, cy + unit * 0.062f, unit * 0.027f, Color.WHITE, false, Paint.Align.CENTER);
        hitX[SUN] = cx; hitY[SUN] = cy; hitN = Math.max(hitN, SUN + 1);

        // planets
        for (int i = 0; i < 8; i++) {
            hitX[i] = -1000; hitY[i] = -1000;
            if (pR[i] > rmaxCur * 1.03) continue;
            float rr = radSolar(pR[i]);
            float x = cx + rr * (float) Math.cos(pTheta[i]), y = cy - rr * (float) Math.sin(pTheta[i]);
            hitX[i] = x; hitY[i] = y;
            float rad = unit * PSIZE[i];
            p.setStyle(Paint.Style.FILL);
            p.setColor(BG);
            c.drawCircle(x, y, rad + unit * 0.005f, p);
            p.setColor(PCOLOR[i]);
            c.drawCircle(x, y, rad, p);
            if (i == 5) { // Saturn's ring
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(unit * 0.004f);
                c.save();
                c.rotate(-20f, x, y);
                c.drawOval(x - rad * 2f, y - rad * 0.6f, x + rad * 2f, y + rad * 0.6f, p);
                c.restore();
                p.setStyle(Paint.Style.FILL);
            }
            if (i == 2) {
                ring(c, x, y, rad + unit * 0.014f, GREEN, unit * 0.005f);
                text(c, "\uD83D\uDCCD Earth \u2014 you are here", x, y - rad - unit * 0.03f, unit * 0.03f, GREEN, true, Paint.Align.CENTER);
            } else {
                text(c, Orbits.NAMES[i], x, y + rad + unit * 0.034f, unit * 0.026f, Color.WHITE, false, Paint.Align.CENTER);
            }
        }
        hitN = 9;

        if (selected >= 0 && selected <= 8) ring(c, hitX[selected], hitY[selected], unit * 0.05f, GOLD, unit * 0.004f);

        text(c, "top view \u2022 distances squeezed so the inner planets fit", cx, h - unit * 0.015f, unit * 0.024f, 0xFF8A8AA8, false, Paint.Align.CENTER);
        text(c, "\u2192 March-equinox direction", w - unit * 0.015f, cy - unit * 0.012f, unit * 0.022f, 0xFF8A8AA8, false, Paint.Align.RIGHT);
    }

    // ------------------------------------------------------------ nearby stars
    private void drawStars(Canvas c, float w, float h) {
        float unit = Math.min(w, h), cx = w / 2f, cy = h / 2f, R = unit * 0.45f;
        double dmax = starsDmax(zoom), d0 = 2.0, k = Math.log(1 + dmax / d0);

        double[] marks = {5, 10, 25, 50, 100, 250, 500, 1000};
        for (double m : marks) {
            if (m > dmax * 1.02) continue;
            float rr = (float) (R * Math.log(1 + m / d0) / k);
            ring(c, cx, cy, rr, GRAYLINE, unit * 0.0025f);
            text(c, (int) m + " ly", cx + rr * 0.707f + unit * 0.01f, cy - rr * 0.707f - unit * 0.005f, unit * 0.022f, 0xFF8A8AA8, false, Paint.Align.LEFT);
        }

        // Galactic centre is straight up, galactic rotation points left
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(unit * 0.003f);
        p.setColor(0xFF5A5A88);
        c.drawLine(cx, cy, cx, cy - R, p);
        c.drawLine(cx, cy, cx - R, cy, p);
        p.setStyle(Paint.Style.FILL);
        text(c, "\u2191 towards the Galactic centre", cx, cy - R - unit * 0.01f, unit * 0.024f, 0xFFB8B8D0, false, Paint.Align.CENTER);
        text(c, "\u2190 Sun's path round the galaxy", cx - R - unit * 0.005f, cy + unit * 0.035f, unit * 0.022f, 0xFFB8B8D0, false, Paint.Align.LEFT);

        for (int i = 0; i < Cosmos.STARS.length; i++) {
            Cosmos.Star s = Cosmos.STARS[i];
            hitX[i] = -1000; hitY[i] = -1000;
            if (s.ly > dmax) continue;
            double dxy = Math.hypot(s.x, s.y);
            double rr = R * Math.log(1 + dxy / d0) / k;
            double ux = dxy < 1e-9 ? 0 : s.x / dxy, uy = dxy < 1e-9 ? 0 : s.y / dxy;
            float x = cx - (float) (uy * rr), y = cy - (float) (ux * rr);
            hitX[i] = x; hitY[i] = y;
            float rad = unit * 0.011f;
            p.setColor(Color.argb(60, Color.red(s.color), Color.green(s.color), Color.blue(s.color)));
            c.drawCircle(x, y, rad * 2f, p);
            p.setColor(s.color);
            c.drawCircle(x, y, rad, p);
            text(c, s.name, x + rad + unit * 0.008f, y + unit * 0.008f, unit * 0.024f, i == selected ? GOLD : Color.WHITE, i == selected, Paint.Align.LEFT);
            if (i == selected) ring(c, x, y, unit * 0.03f, GOLD, unit * 0.004f);
        }
        hitN = Cosmos.STARS.length;

        // you
        p.setShader(new RadialGradient(cx, cy, unit * 0.05f, 0xFFFFE082, 0x00FFC107, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, unit * 0.05f, p);
        p.setShader(null);
        p.setColor(GOLD);
        c.drawCircle(cx, cy, unit * 0.016f, p);
        text(c, "\uD83D\uDCCD Sun & Earth \u2014 you are here", cx, cy + unit * 0.075f, unit * 0.028f, GREEN, true, Paint.Align.CENTER);
        text(c, "Milky Way disk seen from above \u2022 distances squeezed", cx, h - unit * 0.012f, unit * 0.022f, 0xFF8A8AA8, false, Paint.Align.CENTER);
    }

    // ------------------------------------------------------------------ galaxy
    private static final double SUN_R = 26700.0; // light-years from the Galactic centre

    private float gx(double xLy, float cx, double cxLy, double s) { return cx + (float) ((xLy - cxLy) * s); }
    private float gy(double yLy, float cy, double cyLy, double s) { return cy - (float) ((yLy - cyLy) * s); }

    private void drawGalaxy(Canvas c, float w, float h) {
        float unit = Math.min(w, h), cx = w / 2f, cy = h / 2f;
        double base = (unit * 0.46) / 55000.0;
        double s = base * galaxyScale(zoom);
        double cxLy = 0, cyLy = -SUN_R * ((Math.log(galaxyScale(zoom))) / Math.log(8.0)); // glide from the centre to the Sun
        float gcx = gx(0, cx, cxLy, s), gcy = gy(0, cy, cyLy, s);

        // disk and bulge
        p.setStyle(Paint.Style.FILL);
        p.setShader(new RadialGradient(gcx, gcy, (float) (50000 * s), new int[]{0x66385A9A, 0x33203060, 0x00000000}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(gcx, gcy, (float) (50000 * s), p);
        p.setShader(new RadialGradient(gcx, gcy, (float) (7000 * s), 0xCCFFE9A8, 0x00FFE9A8, Shader.TileMode.CLAMP));
        c.drawCircle(gcx, gcy, (float) (7000 * s), p);
        p.setShader(null);

        // bar
        c.save();
        c.rotate(30f, gcx, gcy);
        p.setColor(0x55FFD27E);
        c.drawOval(gcx - (float) (8000 * s), gcy - (float) (1800 * s), gcx + (float) (8000 * s), gcy + (float) (1800 * s), p);
        c.restore();

        // spiral arms (schematic, 4 arms, pitch 15 degrees)
        double tanP = Math.tan(Math.toRadians(15));
        int[] acol = {0xFF7EC8FF, 0xFFFFB3D9, 0xFFFFD27E, 0xFFA8FFB0};
        double[] phi = {-90, -180, 0, 90};
        String[] aname = {"Sagittarius", "Perseus", "Scutum\u2013Centaurus", "Norma\u2013Outer"};
        for (int a = 0; a < 4; a++) {
            path.reset();
            boolean first = true;
            float lx = 0, ly = 0;
            for (double r = 8000; r <= 46000; r += 400) {
                double th = Math.toRadians(phi[a]) + Math.log(r / 21000.0) / tanP;
                float x = gx(r * Math.cos(th), cx, cxLy, s), y = gy(r * Math.sin(th), cy, cyLy, s);
                if (first) { path.moveTo(x, y); first = false; } else path.lineTo(x, y);
                if (Math.abs(r - 41000) < 200) { lx = x; ly = y; }
            }
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth((float) Math.max(unit * 0.012, 3000 * s));
            p.setColor(Color.argb(70, Color.red(acol[a]), Color.green(acol[a]), Color.blue(acol[a])));
            c.drawPath(path, p);
            p.setStrokeWidth(unit * 0.004f);
            p.setColor(Color.argb(200, Color.red(acol[a]), Color.green(acol[a]), Color.blue(acol[a])));
            c.drawPath(path, p);
            if (lx > 0 && lx < w && ly > 0 && ly < h) text(c, aname[a], lx, ly, unit * 0.026f, acol[a], false, Paint.Align.CENTER);
        }
        p.setStyle(Paint.Style.FILL);

        // the Sun's orbit round the galaxy
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(unit * 0.003f);
        p.setColor(0xAAFFFFFF);
        p.setPathEffect(new DashPathEffect(new float[]{unit * 0.02f, unit * 0.015f}, 0));
        c.drawCircle(gcx, gcy, (float) (SUN_R * s), p);
        // line from the centre to the Sun
        float sx = gx(0, cx, cxLy, s), sy = gy(-SUN_R, cy, cyLy, s);
        c.drawLine(gcx, gcy, sx, sy, p);
        p.setPathEffect(null);
        p.setStyle(Paint.Style.FILL);

        // Orion Spur (a short local arm piece at the Sun)
        float spur = (float) (5000 * s);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth((float) Math.max(unit * 0.008, 900 * s));
        p.setColor(0xAA6FE0D0);
        c.drawLine(sx - spur, sy + spur * 0.18f, sx + spur, sy - spur * 0.18f, p);
        p.setStyle(Paint.Style.FILL);
        text(c, "Orion Spur", sx - spur * 0.9f, sy + spur * 0.18f + unit * 0.045f, unit * 0.024f, 0xFF6FE0D0, false, Paint.Align.LEFT);

        // centre and Sun
        p.setColor(Color.WHITE);
        c.drawCircle(gcx, gcy, unit * 0.008f, p);
        text(c, "Galactic centre (Sgr A*)", gcx, gcy - unit * 0.025f, unit * 0.024f, Color.WHITE, false, Paint.Align.CENTER);
        p.setShader(new RadialGradient(sx, sy, unit * 0.045f, 0xFFFFE082, 0x00FFC107, Shader.TileMode.CLAMP));
        c.drawCircle(sx, sy, unit * 0.045f, p);
        p.setShader(null);
        p.setColor(GOLD);
        c.drawCircle(sx, sy, unit * 0.014f, p);
        text(c, "\uD83D\uDCCD Sun & Earth \u2014 you are here", sx, sy + unit * 0.085f, unit * 0.028f, GREEN, true, Paint.Align.CENTER);
        text(c, "26,700 light-years", (gcx + sx) / 2f + unit * 0.015f, (gcy + sy) / 2f, unit * 0.024f, Color.WHITE, false, Paint.Align.LEFT);
        text(c, "schematic view from above the galaxy \u2022 the Sun moves to the left", cx, h - unit * 0.012f, unit * 0.022f, 0xFF8A8AA8, false, Paint.Align.CENTER);
    }

    // ------------------------------------------------------------------ touch
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mode == GALAXY) return false;
        if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (e.getAction() == MotionEvent.ACTION_UP) {
            float unit = Math.min(getWidth(), getHeight());
            int best = -1;
            float bestD = unit * 0.09f;
            for (int i = 0; i < hitN; i++) {
                float d = (float) Math.hypot(e.getX() - hitX[i], e.getY() - hitY[i]);
                if (d < bestD) { bestD = d; best = i; }
            }
            if (best >= 0) {
                selected = best;
                invalidate();
                if (listener != null) listener.onSelect(mode, best);
            }
            return true;
        }
        return true;
    }
}
