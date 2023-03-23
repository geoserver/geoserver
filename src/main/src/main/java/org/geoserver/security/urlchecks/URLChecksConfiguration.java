/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.urlchecks;

import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration of the URL checks subsystem. In particular, currently contains a flag for enabling
 * the checks, and a list of {@link AbstractURLCheck} to be applied.
 */
public class URLChecksConfiguration {

    List<AbstractURLCheck> checks = new ArrayList<>();
    Boolean enabled = true;

    public URLChecksConfiguration(boolean enabled, List<AbstractURLCheck> checks) {
        this.enabled = enabled;
        this.checks = checks;
    }

    public URLChecksConfiguration() {
        this.enabled = true;
        this.checks = new ArrayList<>();
    }

    public List<AbstractURLCheck> getChecks() {
        return checks;
    }

    public void setChecks(List<AbstractURLCheck> checks) {
        this.checks = checks;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Initialize after XStream deserialization */
    private Object readResolve() throws ObjectStreamException {
        if (checks == null) {
            checks = new ArrayList<>();
        }
        if (enabled == null) {
            enabled = true;
        }
        return this;
    }
}
