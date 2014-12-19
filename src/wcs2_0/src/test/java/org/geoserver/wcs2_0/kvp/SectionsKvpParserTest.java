/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static org.junit.Assert.*;
import java.util.List;

import net.opengis.ows20.SectionsType;

import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.Test;

/**
 * Tests the parser for the "Sections" parameter. Version 2.0
 * 
 * @author Nicola Lagomarsini GeoSolutions
 */
public class SectionsKvpParserTest extends WCSKVPTestSupport {

    @Test
    public void testParserForVersion() {
        // look up parser objects
        List<KvpParser> parsers = GeoServerExtensions.extensions(KvpParser.class);
        // Version 2.0.0
        KvpParser parser = KvpUtils.findParser("sections", "WCS", null, "2.0.0", parsers);
        assertNotNull(parser);
        // Ensure the correct parser is taken
        assertEquals(parser.getClass(), SectionsKvpParser.class);
        // Version 2.0.1
        parser = KvpUtils.findParser("sections", "WCS", null, "2.0.1", parsers);
        assertNotNull(parser);
        // Ensure the correct parser is taken
        assertEquals(parser.getClass(), SectionsKvpParser.class);
        // Version not defined
        parser = KvpUtils.findParser("sections", "WCS", null, null, parsers);
        assertNotNull(parser);
        // Ensure the correct parser is taken
        assertEquals(parser.getClass(), SectionsKvpParser.class);
    }

    @Test
    public void testParse() throws Exception {
        // look up parser objects
        List<KvpParser> parsers = GeoServerExtensions.extensions(KvpParser.class);
        // Get the parser
        SectionsKvpParser parser = (SectionsKvpParser) KvpUtils.findParser("sections", "WCS", null,
                "2.0.0", parsers);
        // Parse the object
        Object parsed = parser.parse("all");
        // Ensure the object is not null and is an instance of the SectionsType class
        assertNotNull(parsed);
        assertTrue(parsed instanceof SectionsType);
    }
}
