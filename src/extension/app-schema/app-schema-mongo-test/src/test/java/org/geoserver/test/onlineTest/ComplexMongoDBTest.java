/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import org.junit.Test;
import org.w3c.dom.Document;

/** Tests the integration between MongoDB and App-schema. */
public class ComplexMongoDBTest extends ComplexMongoDBSupport {

    @Override
    protected String getPathOfMappingsToUse() {
        return "/mappings/stations.xml";
    }

    @Test
    public void testAttributeIsNull() throws Exception {
        Document document =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=st:StationFeature"
                                + "&filter=<Filter><PropertyIsNull>"
                                + "<PropertyName>StationFeature/nullableField</PropertyName>"
                                + "</PropertyIsNull></Filter>");
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                2,
                "/wfs:FeatureCollection/gml:featureMembers/st:StationFeature");
    }
}
