/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.filter.GeoServerJ2eeAuthenticationFilter;
import org.geoserver.security.filter.GeoServerX509CertificateAuthenticationFilter;

/**
 * Base config for J2EE based filters ({@link GeoServerJ2eeAuthenticationFilter} and {@link
 * GeoServerX509CertificateAuthenticationFilter})
 *
 * <p>* {@link #getRoleSource()} determines how to calculate the roles:
 *
 * <ol>
 *   <li>{@link J2EERoleSource#UserGroupService} - Roles are calculated using the named user group
 *       service {@link #getUserGroupServiceName()}
 *   <li>{@link J2EERoleSource#RoleService} - Roles are calculated using the named role service
 *       {@link #getRoleServiceName()}. If no role service is given, the default is {@link
 *       GeoServerSecurityManager#getActiveRoleService()}
 *   <li>{@link J2EERoleSource#Header} - Roles are calculated using the content of {@link
 *       #getRolesHeaderAttribute()} parsed by {@link #getRoleConverterName()}. if no converter is
 *       given, roles are parsed by the default converter {@link GeoServerRoleConverter}
 *   <li>{@link J2EERoleSource#J2EE} - Roles are fetched from J2EE container
 *
 * @author Mauro Bartolomeoli (mauro.bartolomeoli@geo-solutions.it)
 */
public class J2eeAuthenticationBaseFilterConfig extends PreAuthenticatedUserNameFilterConfig {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * RoleSource list values extended for filters supporting J2EE RoleSource.
     *
     * @author Mauro Bartolomeoli (mauro.bartolomeoli@geo-solutions.it)
     */
    public static enum J2EERoleSource implements RoleSource {
        Header,
        UserGroupService,
        RoleService,
        J2EE;

        @Override
        public boolean equals(RoleSource other) {
            return other != null && other.toString().equals(toString());
        }
    };
}
