/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.xml;

import org.geoserver.security.config.FileBasedSecurityServiceConfig;

/**
 * Extension of {@link FileBasedSecurityServiceConfig} in which the underlying file stored as XML.
 *
 * @author christian
 */
public class XMLSecurityServiceConfig extends FileBasedSecurityServiceConfig {

    private static final long serialVersionUID = 1L;
    private boolean validating;

    public XMLSecurityServiceConfig() {}

    public XMLSecurityServiceConfig(XMLSecurityServiceConfig other) {
        super(other);
        validating = other.isValidating();
    }

    /** Flag activating/deactivating xml schema validation. */
    public boolean isValidating() {
        return validating;
    }

    /** Sets flag activating/deactivating xml schema validation. */
    public void setValidating(boolean validating) {
        this.validating = validating;
    }
}
