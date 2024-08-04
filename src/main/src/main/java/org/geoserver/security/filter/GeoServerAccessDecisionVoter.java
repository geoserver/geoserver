/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import org.geoserver.platform.ExtensionPriority;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.web.FilterInvocation;

public interface GeoServerAccessDecisionVoter
        extends AccessDecisionVoter<FilterInvocation>, ExtensionPriority {

    @Override
    default int getPriority() {
        return ExtensionPriority.LOWEST;
    }
}
