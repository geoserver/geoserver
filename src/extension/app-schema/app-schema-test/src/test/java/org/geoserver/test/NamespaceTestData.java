/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import java.util.Map;
import org.geoserver.data.test.TestData;

/**
 * {@link TestData} that provides access to a map of namespaces.
 *
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public interface NamespaceTestData extends TestData {

    /**
     * Return a map of namespace prefix to namespace URI; the map may be immutable so do not attempt
     * to modify it.
     *
     * @return map of namespace prefix to namespace URI.
     */
    public Map<String, String> getNamespaces();
}
