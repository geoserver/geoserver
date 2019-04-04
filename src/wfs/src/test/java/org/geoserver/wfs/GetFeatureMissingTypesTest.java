/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.*;

import java.io.File;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class GetFeatureMissingTypesTest extends WFSTestSupport {

    @Before
    public void revert() throws Exception {
        revertLayer(CiteTestData.BUILDINGS);
    }

    @Test
    public void testPostMissingType10() throws Exception {
        // let's remove one property file so that its schema cannot be computed
        // (GEOS-3049)
        File root = getTestData().getDataDirectoryRoot();
        File nsDirectory = new File(root, SystemTestData.BUILDINGS.getPrefix());
        File buildings =
                new File(nsDirectory, SystemTestData.BUILDINGS.getLocalPart() + ".properties");
        assertTrue(buildings.delete());

        // we're requesting another feature type, that should work
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cdf:Other\"> "
                        + "<ogc:PropertyName>cdf:string2</ogc:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);

        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertFalse(featureMembers.getLength() == 0);

        // but if we require buildings itself, it should fail
        xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\""
                        + getLayerId(SystemTestData.BUILDINGS)
                        + "\"/> "
                        + "</wfs:GetFeature>";

        doc = postAsDOM("wfs", xml);

        assertEquals("ServiceExceptionReport", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testPostMissingType11() throws Exception {
        // let's remove one property file so that its schema cannot be computed
        // (GEOS-3049)
        File root = getTestData().getDataDirectoryRoot();
        File nsDirectory = new File(root, SystemTestData.BUILDINGS.getPrefix());
        File buildings =
                new File(nsDirectory, SystemTestData.BUILDINGS.getLocalPart() + ".properties");
        assertTrue(buildings.delete());

        // we're requesting another feature type, that should work
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\"cdf:Other\"> "
                        + "<wfs:PropertyName>cdf:string2</wfs:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);

        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        // but if we require buildings itself, it should fail
        xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.1.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\""
                        + getLayerId(SystemTestData.BUILDINGS)
                        + "\"/> "
                        + "</wfs:GetFeature>";

        doc = postAsDOM("wfs", xml);

        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }
}
