/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import java.io.Serializable;

/** The module settings, saved in the data directory. */
public class Config implements Serializable {

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

    /** @return whether each check writes a result line; failures are logged regardless */
    public boolean isLogUpdateChecks() {
        return logUpdateChecks;
    }

    public void setLogUpdateChecks(boolean logUpdateChecks) {
        this.logUpdateChecks = logUpdateChecks;
    }

    /** @return whether the result lines leave out version numbers */
    public boolean isHideVersionNumbersInLogs() {
        return hideVersionNumbersInLogs;
    }

    public void setHideVersionNumbersInLogs(boolean hideVersionNumbersInLogs) {
        this.hideVersionNumbersInLogs = hideVersionNumbersInLogs;
    }

    /** @return an independent copy */
    public Config copy() {
        Config copy = new Config();
        copy.enabled = this.enabled;
        copy.logUpdateChecks = this.logUpdateChecks;
        copy.hideVersionNumbersInLogs = this.hideVersionNumbersInLogs;
        return copy;
    }
}
