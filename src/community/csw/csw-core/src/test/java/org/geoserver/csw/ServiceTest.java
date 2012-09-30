package org.geoserver.csw;

import static org.junit.Assert.*;

import org.geotools.util.Version;
import org.junit.Test;

/**
 * Checks the service is registered and reachable 
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class ServiceTest extends CSWTestSupport {

    @Test
    public void testServiceAvailable() {
        CSWInfo csw = getGeoServer().getService(CSWInfo.class);
        assertEquals(1, csw.getVersions().size());
        assertEquals(new Version("2.0.2"), csw.getVersions().get(0));
    }

}
