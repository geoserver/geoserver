/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A released GeoServer version, {@code MAJOR.MINOR.PATCH}, compared numerically: {@code 2.9.0} sorts below
 * {@code 2.10.0}, which a string comparison gets wrong.
 */
public final class UpdatesCheckerVersion implements Comparable<UpdatesCheckerVersion> {

    private static final Pattern RELEASE = Pattern.compile("^(\\d{1,9})\\.(\\d{1,9})\\.(\\d{1,9})$");

    private static final Pattern SERIES = Pattern.compile("^(\\d{1,9})\\.(\\d{1,9})\\.x$");

    private final int major;

    private final int minor;

    private final int patch;

    public UpdatesCheckerVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /** @return the version, or {@code null} unless {@code text} is exactly {@code MAJOR.MINOR.PATCH}. */
    public static UpdatesCheckerVersion parse(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = RELEASE.matcher(text.trim());
        if (!m.matches()) {
            return null;
        }
        return new UpdatesCheckerVersion(
                Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
    }

    /** @return whether {@code text} is a series key such as {@code "2.27.x"}. */
    public static boolean isSeries(String text) {
        return text != null && SERIES.matcher(text).matches();
    }

    /** Compares two series keys such as {@code "2.9.x"} and {@code "2.10.x"} numerically. Both must be valid. */
    public static int compareSeries(String a, String b) {
        Matcher ma = SERIES.matcher(a);
        Matcher mb = SERIES.matcher(b);
        if (!ma.matches() || !mb.matches()) {
            throw new IllegalArgumentException("Not a series key: " + a + " / " + b);
        }
        int c = Integer.compare(Integer.parseInt(ma.group(1)), Integer.parseInt(mb.group(1)));
        return c != 0 ? c : Integer.compare(Integer.parseInt(ma.group(2)), Integer.parseInt(mb.group(2)));
    }

    /** @return the series this version belongs to, e.g. {@code "2.27.x"}. */
    public String getSeries() {
        return major + "." + minor + ".x";
    }

    @Override
    public int compareTo(UpdatesCheckerVersion other) {
        int c = Integer.compare(major, other.major);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(minor, other.minor);
        return c != 0 ? c : Integer.compare(patch, other.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UpdatesCheckerVersion)) {
            return false;
        }
        UpdatesCheckerVersion other = (UpdatesCheckerVersion) o;
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override
    public int hashCode() {
        return (major * 31 + minor) * 31 + patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
