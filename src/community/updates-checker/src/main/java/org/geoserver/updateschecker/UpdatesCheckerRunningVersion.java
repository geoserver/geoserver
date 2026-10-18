/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The version this GeoServer is running, classified for the update check: a plain release that can be compared, a
 * development or pre-release build that gets an informational notice only, or a version that could not be determined.
 * "Not determined" is an explicit state, never a placeholder string.
 */
public final class UpdatesCheckerRunningVersion {

    /** How the running version is treated by the update check. */
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

    private final UpdatesCheckerVersion release;

    private UpdatesCheckerRunningVersion(Kind kind, String raw, UpdatesCheckerVersion release) {
        this.kind = kind;
        this.raw = raw;
        this.release = release;
    }

    /** @param raw the manifest {@code Implementation-Version}, or {@code null} if it could not be read. */
    public static UpdatesCheckerRunningVersion of(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new UpdatesCheckerRunningVersion(Kind.UNKNOWN, null, null);
        }
        String text = raw.trim();
        Matcher m = VERSION.matcher(text);
        if (!m.matches()) {
            return new UpdatesCheckerRunningVersion(Kind.UNKNOWN, text, null);
        }
        UpdatesCheckerVersion plain = UpdatesCheckerVersion.parse(text);
        if (plain != null) {
            return new UpdatesCheckerRunningVersion(Kind.RELEASE, text, plain);
        }
        // anything else that starts like a version - a pre-release qualifier or no patch component - is a
        // development build; no comparison against released versions is attempted for it
        return new UpdatesCheckerRunningVersion(Kind.DEVELOPMENT, text, null);
    }

    public Kind getKind() {
        return kind;
    }

    /** @return the text to show to the user, or {@code null} when the version is not determined. */
    public String getRaw() {
        return raw;
    }

    /** @return the comparable version, or {@code null} unless {@link #getKind()} is {@link Kind#RELEASE}. */
    public UpdatesCheckerVersion getRelease() {
        return release;
    }

    public boolean isRelease() {
        return kind == Kind.RELEASE;
    }
}
