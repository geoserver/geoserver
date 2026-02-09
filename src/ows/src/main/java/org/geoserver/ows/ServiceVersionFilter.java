/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.List;
import org.geoserver.platform.Service;

/** Extension point for filtering service versions based on configuration. */
public interface ServiceVersionFilter {

    /**
     * Filters a list of supported versions, removing any that are disabled.
     *
     * @param service The service being requested (used to look up configuration)
     * @param versions The full list of supported versions
     * @return The filtered list with disabled versions removed
     */
    List<String> filterVersions(Service service, List<String> versions);
}
