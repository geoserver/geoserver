/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.Serializable;

/** The combination of access level granted and response policy (lists only possible cases) */
public class WrapperPolicy implements Serializable, Comparable<WrapperPolicy> {
    private static final long serialVersionUID = -7490634837165130290L;

    // TODO: turn these into private fields
    public final AccessLevel level; // needed, depends on catalog mode and request type
    public final Response response; // needed, by catalog mode
    public final AccessLimits limits;

    public static final WrapperPolicy hide(AccessLimits limits) {
        return new WrapperPolicy(AccessLevel.HIDDEN, Response.HIDE, limits);
    }

    public static final WrapperPolicy metadata(AccessLimits limits) {
        return new WrapperPolicy(AccessLevel.METADATA, Response.CHALLENGE, limits);
    }

    public static final WrapperPolicy readOnlyChallenge(AccessLimits limits) {
        return new WrapperPolicy(AccessLevel.READ_ONLY, Response.CHALLENGE, limits);
    }

    public static final WrapperPolicy readOnlyHide(AccessLimits limits) {
        return new WrapperPolicy(AccessLevel.READ_ONLY, Response.HIDE, limits);
    }

    public static final WrapperPolicy readWrite(AccessLimits limits) {
        return new WrapperPolicy(AccessLevel.READ_WRITE, Response.HIDE, limits);
    }

    WrapperPolicy(AccessLevel level, Response response, AccessLimits limits) {
        this.level = level;
        this.response = response;
        this.limits = limits;
    }

    public Response getResponse() {
        return response;
    }

    public AccessLimits getLimits() {
        return limits;
    }

    public AccessLevel getAccessLevel() {
        return level;
    }

    public boolean isHide() {
        return level == AccessLevel.HIDDEN && response == Response.HIDE;
    }

    public boolean isMetadata() {
        return level == AccessLevel.METADATA && response == Response.CHALLENGE;
    }

    public boolean isReadOnlyChallenge() {
        return level == AccessLevel.READ_ONLY && response == Response.CHALLENGE;
    }

    public boolean isReadOnlyHide() {
        return level == AccessLevel.READ_ONLY && response == Response.HIDE;
    }

    public boolean isReadWrite() {
        return level == AccessLevel.READ_ONLY && response == Response.CHALLENGE;
    }

    /** Builds a new WrapperPolicy copying this one, but with a different access limits object */
    public WrapperPolicy derive(AccessLimits limits) {
        return new WrapperPolicy(this.level, this.response, limits);
    }

    /**
     * Sorts wrapper policies from more to less restrictive limits.
     *
     * <p>That is, first comparison order is {@link #getAccessLevel() getAccessLevel() ==} {@link
     * AccessLevel#HIDDEN HIDDEN}/{@link AccessLevel#METADATA METADATA}/ {@link
     * AccessLevel#READ_ONLY READ_ONLY}/{@link AccessLevel#READ_WRITE READ_WRITE}.
     *
     * <p>Second comparison criteria is {@link AccessLimits#getMode() getLimits().getMode()} {@code
     * == } {@link CatalogMode#HIDE HIDE}/ {@link CatalogMode#CHALLENGE CHALLENGE}/ {@link
     * CatalogMode#MIXED MIXED}/{@link #getLimits() getLimits() == null} (i.e. no limits)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(WrapperPolicy w) {
        int levelComparison = getAccessLevel().compareTo(w.getAccessLevel());
        if (levelComparison != 0) {
            return levelComparison;
        }
        CatalogMode myLimits = getLimits() == null ? null : getLimits().getMode();
        CatalogMode theirLimits = w.getLimits() == null ? null : w.getLimits().getMode();

        return myLimits == null
                ? (theirLimits == null ? 0 : 1)
                : (theirLimits == null ? -1 : myLimits.compareTo(theirLimits));
    }

    @Override
    public String toString() {
        return "WrapperPolicy [level="
                + level
                + ", response="
                + response
                + ", limits="
                + limits
                + "]";
    }
}
