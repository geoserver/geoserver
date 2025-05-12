/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.rest.security.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RoleSource;

// This class is needed to fix the issue with RoleSource issues
public class RoleSourceAdapter
        extends XmlAdapter<PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource, RoleSource> {
    @Override
    public RoleSource unmarshal(PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource v) {
        return v; // Convert XML enum back to interface
    }

    @Override
    public PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource marshal(RoleSource v) {
        return (PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource)
                v; // Convert interface to concrete enum
    }
}
