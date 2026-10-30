/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

/** The feed could not be downloaded or understood. */
public class FeedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Text reason;

    public FeedException(Text reason) {
        super(reason.base());
        this.reason = reason;
    }

    public FeedException(Text reason, Throwable cause) {
        super(reason.base(), cause);
        this.reason = reason;
    }

    public Text getReason() {
        return reason;
    }
}
