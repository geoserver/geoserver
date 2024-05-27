/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.urlchecks;

import org.geotools.data.ows.URLChecker;

/**
 * Base class for configurable URL checks. It's {@link URLChecker}, but since it's configurable at
 * runtime, it will be created/destroyed during the life of the application. Also, there will be
 * multiple instances from the same check class. As a result, it's not well suited for direct SPI
 * usage. {@link GeoServerURLChecker} is the stable checker plugged into SPI, which then looks up
 * and uses implementations of this class hierarchy.
 */
public abstract class AbstractURLCheck implements URLChecker {
    protected String name;
    protected String description;
    protected boolean enabled = true;

    public AbstractURLCheck() {
        super();
    }

    /** @return the name */
    @Override
    public String getName() {
        return name;
    }

    /** @param name the name to set */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the description */
    public String getDescription() {
        return description;
    }

    /** @param description the description to set */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the enabled status */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** @param enabled the enabled status to set */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * A short string representation of the configuration of this checker, to be used for generic
     * display purposes
     */
    public abstract String getConfiguration();
}
