package com.blackfalcon.earthinspace;

import java.util.Locale;

/** Nearby stars and the "cosmic address" ladder. */
public final class Cosmos {
    private Cosmos() { }

    public static final class Star {
        public final String name;
        public final double ra, dec, ly, l, b, x, y; // x toward the Galactic centre, y toward galactic rotation (ly, in the plane)
        public final int color;
        Star(String name, double raHours, double dec, double ly, int color) {
            this.name = name; this.ra = raHours * 15.0; this.dec = dec; this.ly = ly; this.color = color;
            double[] g = Orbits.toGalactic(this.ra, dec);
            this.l = g[0]; this.b = g[1];
            this.x = ly * Math.cos(Math.toRadians(b)) * Math.cos(Math.toRadians(l));
            this.y = ly * Math.cos(Math.toRadians(b)) * Math.sin(Math.toRadians(l));
        }
    }

    private static final int WHITE = 0xFFFFFFFF, BLUEWHITE = 0xFFCFE3FF, YELLOW = 0xFFFFE08A, ORANGE = 0xFFFFA64D, RED = 0xFFFF6B5A;

    public static final Star[] STARS = {
            new Star("Alpha Centauri", 14.660, -60.835, 4.37, YELLOW),
            new Star("Barnard's Star", 17.963, 4.693, 5.96, RED),
            new Star("Wolf 359", 10.941, 7.01, 7.86, RED),
            new Star("Lalande 21185", 11.056, 35.97, 8.31, RED),
            new Star("Sirius", 6.7525, -16.716, 8.6, BLUEWHITE),
            new Star("Epsilon Eridani", 3.549, -9.458, 10.5, ORANGE),
            new Star("Procyon", 7.655, 5.225, 11.46, WHITE),
            new Star("Tau Ceti", 1.734, -15.937, 11.9, YELLOW),
            new Star("Altair", 19.846, 8.868, 16.7, WHITE),
            new Star("Vega", 18.616, 38.784, 25.0, BLUEWHITE),
            new Star("Fomalhaut", 22.961, -29.622, 25.1, WHITE),
            new Star("Pollux", 7.755, 28.026, 33.8, ORANGE),
            new Star("Arcturus", 14.261, 19.182, 36.7, ORANGE),
            new Star("Capella", 5.278, 45.998, 42.9, YELLOW),
            new Star("Aldebaran", 4.599, 16.509, 65, ORANGE),
            new Star("Regulus", 10.139, 11.967, 79, BLUEWHITE),
            new Star("Spica", 13.420, -11.161, 250, BLUEWHITE),
            new Star("Polaris", 2.530, 89.264, 433, YELLOW),
            new Star("Betelgeuse", 5.919, 7.407, 550, RED),
            new Star("Antares", 16.490, -26.432, 550, RED),
            new Star("Rigel", 5.242, -8.202, 860, BLUEWHITE)
    };

    public static final class Level {
        public final String emoji, name, size, desc;
        public final double meters;
        Level(String emoji, String name, String size, double meters, String desc) {
            this.emoji = emoji; this.name = name; this.size = size; this.meters = meters; this.desc = desc;
        }
    }

    private static final double AU = 1.495978707e11, LY = 9.4607e15;

    public static final Level[] LADDER = {
            new Level("\uD83C\uDF0D", "Earth", "12,742 km wide", 1.2742e7,
                    "Your home. Everything you have ever known is on this one small ball."),
            new Level("\uD83C\uDF19", "Earth\u2013Moon system", "the Moon orbits at 384,400 km", 7.688e8,
                    "Light takes about 1.3 seconds to reach the Moon."),
            new Level("\u2600\uFE0F", "Solar System (planets)", "Neptune's orbit is about 60 AU across", 2 * 30.07 * AU,
                    "The Sun and its 8 planets. Sunlight takes about 8 minutes 20 seconds to reach Earth."),
            new Level("\uD83D\uDEF0\uFE0F", "Heliosphere", "about 240 AU across", 2 * 120 * AU,
                    "The Sun's bubble of solar wind. Voyager 1 crossed its edge at about 121 AU (2012), Voyager 2 at about 119 AU (2018)."),
            new Level("\u2604\uFE0F", "Oort Cloud (never seen directly)", "up to about 100,000 AU from the Sun (1.6 light-years)", 2 * 1.0e5 * AU,
                    "A huge shell of icy bodies that is thought to be where long-period comets come from."),
            new Level("\uD83C\uDF2B\uFE0F", "Local Interstellar Cloud", "about 30 light-years across", 30 * LY,
                    "A thin wisp of gas the Sun is passing through right now."),
            new Level("\uD83E\uDEE7", "Local Bubble", "about 1,000 light-years across", 1000 * LY,
                    "A giant cavity of hot, thin gas blown out by supernovae. The Sun sits inside it."),
            new Level("\uD83C\uDF00", "Orion Spur (Local Arm)", "about 3,500 light-years wide, 10,000 long", 3500 * LY,
                    "A small arm piece between the Sagittarius and Perseus arms of the Milky Way. This is Earth's neighbourhood."),
            new Level("\uD83C\uDF0C", "Milky Way galaxy", "about 100,000 light-years across", 1.0e5 * LY,
                    "Hundreds of billions of stars. The Sun is about 26,700 light-years from the centre."),
            new Level("\uD83D\uDD2D", "Local Group", "about 10 million light-years across", 1.0e7 * LY,
                    "The Milky Way, Andromeda (about 2.5 million light-years away), Triangulum and dozens of smaller galaxies."),
            new Level("\uD83C\uDF20", "Virgo Supercluster", "about 110 million light-years across", 1.1e8 * LY,
                    "A huge collection of galaxy groups. Our Local Group sits on its edge."),
            new Level("\uD83D\uDD78\uFE0F", "Laniakea Supercluster", "about 520 million light-years across", 5.2e8 * LY,
                    "Our home supercluster, mapped in 2014 from how galaxies flow. Everything in it drifts toward a gravity centre called the Great Attractor."),
            new Level("\uD83C\uDF10", "Observable Universe", "about 93 billion light-years across", 9.3e10 * LY,
                    "Everything whose light has had time to reach us since the Big Bang about 13.8 billion years ago. Every observer has their own observable universe, with themselves at the centre.")
    };

    public static String lightTime(double meters) {
        double s = meters / 299792458.0;
        if (s < 1) return String.format(Locale.US, "%.0f milliseconds", s * 1000);
        if (s < 120) return String.format(Locale.US, "%.0f seconds", s);
        if (s < 7200) return String.format(Locale.US, "%.0f minutes", s / 60);
        if (s < 172800) return String.format(Locale.US, "%.0f hours", s / 3600);
        double days = s / 86400;
        if (days < 730) return String.format(Locale.US, "%.0f days", days);
        double years = days / 365.25;
        if (years < 1e6) return String.format(Locale.US, "%,.0f years", years);
        if (years < 1e9) return String.format(Locale.US, "%,.1f million years", years / 1e6);
        return String.format(Locale.US, "%,.1f billion years", years / 1e9);
    }
}
