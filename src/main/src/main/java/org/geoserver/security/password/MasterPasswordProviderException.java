/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import org.geoserver.security.validation.SecurityConfigException;

/**
 * Base class for master password provider config exceptions.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MasterPasswordProviderException extends SecurityConfigException {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public MasterPasswordProviderException(String errorId, Object... args) {
        super(errorId, args);
    }

    public MasterPasswordProviderException(String errorId, String message, Object... args) {
        super(errorId, message, args);
    }
}
