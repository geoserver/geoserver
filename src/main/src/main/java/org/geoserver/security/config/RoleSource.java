/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig.J2EERoleSource;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;

/**
 * RoleSource interface for {@link PreAuthenticaticatedUserNameFilterConfig}
 *
 * @author Mauro Bartolomeoli (mauro.bartolomeoli@geo-solutions.it)
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonSubTypes(
        value = {
            @JsonSubTypes.Type(value = J2EERoleSource.class),
            @JsonSubTypes.Type(value = PreAuthenticatedUserNameRoleSource.class),
        })
public interface RoleSource {
    /** We need a method to compare different RoleSource. */
    public boolean equals(RoleSource other);
}
