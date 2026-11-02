/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A released GeoServer version, {@code MAJOR.MINOR.PATCH}, compared numerically, so 2.9.0 sorts below 2.10.0. */
public final class Version implements Comparable<Version> {

    private static final Pattern RELEASE = Pattern.compile("^(\\d{1,9})\\.(\\d{1,9})\\.(\\d{1,9})$");

    private static final Pattern SERIES = Pattern.compile("^(\\d{1,9})\\.(\\d{1,9})\\.x$");

    private final int major;

    private final int minor;

    private final int patch;

    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /** @return the version, or {@code null} unless the text is exactly {@code MAJOR.MINOR.PATCH} */
    public static Version parse(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = RELEASE.matcher(text.trim());
        if (!m.matches()) {
            return null;
        }
        return new Version(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
    }

    /** @return whether the text is a series key, such as {@code 2.27.x} */
    public static boolean isSeries(String text) {
        return text != null && SERIES.matcher(text).matches();
    }

    /** Compares two valid series keys numerically. */
    public static int compareSeries(String a, String b) {
        Matcher ma = SERIES.matcher(a);
        Matcher mb = SERIES.matcher(b);
        if (!ma.matches() || !mb.matches()) {
            throw new IllegalArgumentException("Not a series key: " + a + " / " + b);
        }
        int c = Integer.compare(Integer.parseInt(ma.group(1)), Integer.parseInt(mb.group(1)));
        return c != 0 ? c : Integer.compare(Integer.parseInt(ma.group(2)), Integer.parseInt(mb.group(2)));
    }

    /** @return the series key, such as {@code 2.27.x} */
    public String getSeries() {
        return major + "." + minor + ".x";
    }

    @Override
    public int compareTo(Version other) {
        int c = Integer.compare(major, other.major);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(minor, other.minor);
        return c != 0 ? c : Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
