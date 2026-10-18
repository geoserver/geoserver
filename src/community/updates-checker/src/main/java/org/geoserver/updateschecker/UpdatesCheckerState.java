/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

/**
 * What a check tells the administrator to do. The constant names are the words written to the log and returned by the
 * REST endpoint, so they must not be renamed.
 */
public enum UpdatesCheckerState {
    /** A critical or high advisory applies or could not be ruled out, or a newer release is a security release. */
    URGENT_UPDATE_AVAILABLE,
    /** Any other advisory, a newer patch or an unsupported series. */
    UPDATE_AVAILABLE,
    /** Nothing needs action. */
    UP_TO_DATE,
    /** The feed could not be read, or the running version is unknown. */
    CHECK_FAILED
}
