/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;

/**
 * Filter chain for services ( NO GUI)
 *
 * @author christian
 */
public class ServiceLoginFilterChain extends VariableFilterChain {

    /** */
    private static final long serialVersionUID = 1L;

    public ServiceLoginFilterChain(String... patterns) {
        super(patterns);
    }

    public SortedSet<String> listFilterCandidates(GeoServerSecurityManager m) throws IOException {
        SortedSet<String> result = new TreeSet<String>();
        for (String filterName : m.listFilters(GeoServerAuthenticationFilter.class)) {
            GeoServerAuthenticationFilter filter =
                    (GeoServerAuthenticationFilter) m.loadFilter(filterName);
            if (filter.applicableForServices()) result.add(filterName);
        }
        return result;
    }
}
