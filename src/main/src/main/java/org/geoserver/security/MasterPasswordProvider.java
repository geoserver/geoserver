/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.geoserver.security.validation.MasterPasswordChangeValidator;

/**
 * Extension point for providing the master password. Instances of this interface are provided via
 * spring context as a strategy for providing the GeoServer master password.
 *
 * <p>Extensions of this interface <b>must</b> be final to prevent an attacker from registering a
 * subclass that could be used to obtain the plain text version of the master password.
 *
 * @author christian
 */
public abstract class MasterPasswordProvider extends AbstractGeoServerSecurityService {

    /**
     * Getter the master password in plain text.
     *
     * <p>This method is package visibility only to prevent extensions from obtaining the master
     * password in plain text.
     */
    final char[] getMasterPassword() throws Exception {
        return doGetMasterPassword();
    }

    /** Internal getter for plain text master password. */
    protected abstract char[] doGetMasterPassword() throws Exception;

    /** Setter for the master password in plain text. */
    final void setMasterPassword(char[] newPasswd) throws Exception {
        doSetMasterPassword(newPasswd);
    }

    /** Internal setter for plain text master password. */
    protected abstract void doSetMasterPassword(char[] passwd) throws Exception;

    public MasterPasswordChangeValidator createPasswordChangeValidator() {
        return new MasterPasswordChangeValidator(getSecurityManager());
    }
}
