/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.password;

import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.validation.PasswordValidatorImpl;

/**
 * Validates a password based on {@link PasswordPolicyConfig} object
 *
 * <p>At a bare minimum, <code>null</code> passwords should not be allowed.
 *
 * <p>Additionally, password must not start with prefixes used by the {@link
 * GeoServerPasswordEncoder} objects To get the prefixes use <code>
 * for (GeoserverPasswordEncoder enc : GeoServerExtensions.extensions(
 *           GeoserverPasswordEncoder.class)) {
 *     System.out.println(enc.getPrefix()+GeoserverPasswordEncoder.PREFIX_DELIMTER);
 *         }
 * </code> A concrete example can be found in {@link PasswordValidatorImpl#PasswordValidatorImpl()}
 *
 * @author christian
 */
public interface PasswordValidator {

    public static final String DEFAULT_NAME = "default";
    public static final String MASTERPASSWORD_NAME = "master";

    /** Setter for the config */
    void setConfig(PasswordPolicyConfig config);

    /** Getter for the config */
    PasswordPolicyConfig getConfig();

    /** Validates the password, throws an exception if the password is not valid */
    void validatePassword(char[] password) throws PasswordPolicyException;
}
