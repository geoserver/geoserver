/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The version this GeoServer runs: a release that can be compared, a development build, or unknown. */
public final class RunningVersion {

    public enum Kind {
        /** A plain {@code MAJOR.MINOR.PATCH} release. */
        RELEASE,
        /** A SNAPSHOT, RC, milestone, beta or alpha build, or one with no patch component. */
        DEVELOPMENT,
        /** No manifest, or a value that does not look like a version. */
        UNKNOWN
    }

    private static final Pattern VERSION = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:[-.](.+))?$");

    private final Kind kind;

    private final String raw;

    private final Version release;

    private RunningVersion(Kind kind, String raw, Version release) {
        this.kind = kind;
        this.raw = raw;
        this.release = release;
    }

    /** @param raw the manifest {@code Implementation-Version}, or {@code null} */
    public static RunningVersion of(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new RunningVersion(Kind.UNKNOWN, null, null);
        }
        String text = raw.trim();
        Matcher m = VERSION.matcher(text);
        if (!m.matches()) {
            return new RunningVersion(Kind.UNKNOWN, text, null);
        }
        Version plain = Version.parse(text);
        if (plain != null) {
            return new RunningVersion(Kind.RELEASE, text, plain);
        }
        return new RunningVersion(Kind.DEVELOPMENT, text, null);
    }

    public Kind getKind() {
        return kind;
    }

    /** @return the version text, or {@code null} when it is not known */
    public String getRaw() {
        return raw;
    }

    /** @return the comparable version, or {@code null} unless the kind is {@link Kind#RELEASE} */
    public Version getRelease() {
        return release;
    }

    public boolean isRelease() {
        return kind == Kind.RELEASE;
    }
}
