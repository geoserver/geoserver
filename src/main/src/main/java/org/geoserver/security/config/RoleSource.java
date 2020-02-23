/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

/**
 * RoleSource interface for {@link PreAuthenticaticatedUserNameFilterConfig}
 *
 * @author Mauro Bartolomeoli (mauro.bartolomeoli@geo-solutions.it)
 */
public interface RoleSource {
    /** We need a method to compare different RoleSource. */
    public boolean equals(RoleSource other);
}
