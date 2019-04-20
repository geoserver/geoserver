/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** @author Davide Savazzi - geo-solutions.it */
public class XMLFeatureInfoOutputFormatTest extends WMSTestSupport {

    @Test
    public void testXmlGetFeatureInfo() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + XMLFeatureInfoOutputFormat.FORMAT;

        MockHttpServletResponse response = getAsServletResponse(request);

        // MimeType
        assertEquals(XMLFeatureInfoOutputFormat.FORMAT, response.getContentType());

        // Content
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo(
                "109", "//wfs:FeatureCollection/gml:featureMembers/cite:Forests/cite:FID", dom);
        assertXpathEvaluatesTo(
                "Green Forest",
                "//wfs:FeatureCollection/gml:featureMembers/cite:Forests/cite:NAME",
                dom);
    }
}
