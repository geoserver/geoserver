/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.io.Serializable;

/**
 * Configuration for the Check for Updates module. Defaults apply to new installs; a saved file keeps its own values.
 */
public class UpdatesCheckerConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean enabled = true;

    private boolean logUpdateChecks = true;

    private boolean hideVersionNumbersInLogs = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Whether one result line is written to the GeoServer log for each check, whichever trigger (server startup, the
     * first view of an admin session, Check Now or the REST endpoint) performed it. Failures to fetch or parse the feed
     * are logged at WARN regardless of this setting.
     */
    public boolean isLogUpdateChecks() {
        return logUpdateChecks;
    }

    public void setLogUpdateChecks(boolean logUpdateChecks) {
        this.logUpdateChecks = logUpdateChecks;
    }

    /**
     * Whether the per-check result lines omit version numbers. The banner and the Status section always show them; this
     * only affects those log lines, not failure lines.
     */
    public boolean isHideVersionNumbersInLogs() {
        return hideVersionNumbersInLogs;
    }

    public void setHideVersionNumbersInLogs(boolean hideVersionNumbersInLogs) {
        this.hideVersionNumbersInLogs = hideVersionNumbersInLogs;
    }

    /**
     * A fresh, independent copy. Used by {@link UpdatesCheckerConfigPersister} so a cached instance can be handed out
     * without callers (e.g. an admin's in-progress, unsaved form edits) mutating the shared cache directly.
     */
    public UpdatesCheckerConfig copy() {
        UpdatesCheckerConfig copy = new UpdatesCheckerConfig();
        copy.enabled = this.enabled;
        copy.logUpdateChecks = this.logUpdateChecks;
        copy.hideVersionNumbersInLogs = this.hideVersionNumbersInLogs;
        return copy;
    }
}
