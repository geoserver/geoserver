/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.security;

import java.util.logging.Logger;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.process.ProcessFilter;
import org.geotools.process.ProcessFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** A process filter based on the security subsystem */
public class SecurityProcessFilter implements ProcessFilter, ExtensionPriority {

    protected static final Logger LOGGER = Logging.getLogger(SecurityProcessFilter.class);
    protected ProcessAccessManager manager;

    @Override
    public ProcessFactory filterFactory(ProcessFactory pf) {
        if (manager == null) {
            manager = GeoServerExtensions.bean(ProcessAccessManager.class);
            if (manager == null) {
                manager =
                        new DefaultProcessAccessManager(
                                GeoServerExtensions.bean(WpsAccessRuleDAO.class));
            }
        }
        return new SecurityProcessFactory(pf, this);
    }

    @Override
    public int getPriority() {
        // Be the last process filter in the list (the sorting is done low to high)
        // This is done to allow other filters to recognize the classes this extension would wrap
        return Integer.MAX_VALUE;
    }

    protected boolean allowProcess(Name processName) {
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        return manager.getAccessLimits(user, processName).isAllowed();
    }
}
