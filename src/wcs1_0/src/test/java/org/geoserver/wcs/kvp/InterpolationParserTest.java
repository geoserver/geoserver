/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.junit.Assert.*;

import java.util.List;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/**
 * Simple test class to use for checking that the interpolation parser used by the version 1.0.0 is
 * correct.
 *
 * @author Nicola Lagomarsini geosolutions
 */
public class InterpolationParserTest extends GeoServerSystemTestSupport {

    @Test
    public void testParserForVersion() {
        // look up parser objects
        List<KvpParser> parsers = GeoServerExtensions.extensions(KvpParser.class);
        KvpParser parser = KvpUtils.findParser("interpolation", "WCS", null, "1.0.0", parsers);
        assertNotNull(parser);
        // Ensure the correct parser is taken
        assertEquals(parser.getClass(), InterpolationMethodKvpParser.class);
    }
}
