/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS GetFeature to test duplicate properties with GeoServer.
 *
 * @author Florence Tan, CSIRO Earth Science and Resource Engineering
 */
public class DuplicatePropertiesWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected DuplicatePropertiesMockData createTestData() {
        return new DuplicatePropertiesMockData();
    }

    /** Test whether GetCapabilities returns wfs:WFS_Capabilities. */
    @Test
    public void testGetCapabilities() {
        Document doc = getAsDOM("wfs?request=GetCapabilities&version=1.1.0");
        LOGGER.info("WFS GetCapabilities response:\n" + prettyString(doc));
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());
        // make sure non-feature types don't appear in FeatureTypeList
        assertXpathCount(1, "//wfs:FeatureType", doc);
        ArrayList<String> featureTypeNames = new ArrayList<String>(1);
        featureTypeNames.add(evaluate("//wfs:FeatureType[1]/wfs:Name", doc));
        // ERM
        assertTrue(featureTypeNames.contains("ex:ERM"));
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&typename=ex:ERM");
        LOGGER.info("WFS GetFeature&typename=ex:ERM response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathCount(2, "//ex:purpose", doc);
        assertXpathEvaluatesTo("instance", "//ex:material/ex:RockMaterial/ex:purpose", doc);
    }
}
