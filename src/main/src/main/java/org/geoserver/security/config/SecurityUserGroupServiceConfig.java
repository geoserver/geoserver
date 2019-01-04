/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import org.geoserver.security.GeoServerUserGroupService;

/**
 * Interface for {@link GeoServerUserGroupService} configuration objects.
 *
 * @author christian
 */
public interface SecurityUserGroupServiceConfig extends SecurityNamedServiceConfig {

    /** The name of the {@link GeoServerPasswordEncoder} used by the service. */
    String getPasswordEncoderName();

    /** Sets the name of the {@link GeoServerPasswordEncoder} used by the service. */
    void setPasswordEncoderName(String passwordEncoderName);

    /** The name of the {@link PasswordValidator} used by the service. */
    String getPasswordPolicyName();

    /** Sets the name of the {@link PasswordValidator} used by the service. */
    void setPasswordPolicyName(String passwordPolicyName);
}
