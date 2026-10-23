/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

/** The feed could not be downloaded or understood; the reason is text that can be shown in the reader's language. */
public class UpdatesCheckerFeedException extends Exception {

    private static final long serialVersionUID = 1L;

    private final UpdatesCheckerText reason;

    public UpdatesCheckerFeedException(UpdatesCheckerText reason) {
        super(reason.base());
        this.reason = reason;
    }

    public UpdatesCheckerFeedException(UpdatesCheckerText reason, Throwable cause) {
        super(reason.base(), cause);
        this.reason = reason;
    }

    /** @return why the feed could not be used. */
    public UpdatesCheckerText getReason() {
        return reason;
    }
}
