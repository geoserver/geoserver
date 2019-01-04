/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.v1_1_0.WFS;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.xsd.Parser;
import org.junit.Test;

public class WFSXmlTest extends WFSTestSupport {

    WFSConfiguration configuration() {
        FeatureTypeSchemaBuilder sb = new FeatureTypeSchemaBuilder.GML3(getGeoServer());
        return new WFSConfiguration(getGeoServer(), sb, new WFS(sb));
    }

    @Test
    public void testValid() throws Exception {
        Parser parser = new Parser(configuration());
        parser.parse(getClass().getResourceAsStream("GetFeature.xml"));

        assertEquals(0, parser.getValidationErrors().size());
    }

    @Test
    public void testInvalid() throws Exception {
        Parser parser = new Parser(configuration());
        parser.setValidating(true);
        parser.parse(getClass().getResourceAsStream("GetFeature-invalid.xml"));

        assertTrue(parser.getValidationErrors().size() > 0);
    }
}
