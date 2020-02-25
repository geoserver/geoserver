/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import static org.junit.Assert.*;

import org.geotools.util.Version;
import org.junit.Test;

/**
 * Checks the service is registered and reachable
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ServiceTest extends CSWSimpleTestSupport {

    @Test
    public void testServiceAvailable() {
        CSWInfo csw = getGeoServer().getService(CSWInfo.class);
        assertEquals(1, csw.getVersions().size());
        assertEquals(new Version("2.0.2"), csw.getVersions().get(0));
    }
}
