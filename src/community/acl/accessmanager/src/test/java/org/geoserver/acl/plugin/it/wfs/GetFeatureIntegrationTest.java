/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.it.wfs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.acl.domain.rules.GrantType.ALLOW;
import static org.geoserver.acl.domain.rules.GrantType.DENY;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests WFS GetFeature integration with ACL rules
 *
 * <p>Test data for reference:
 *
 * <pre>
 * iau:MarsPoi:
 * mars.1
 *   geom: POINT (-36.897 -27.2282)
 *   name: Blunck
 *   diameter: 66.485
 * mars.2
 *   geom: POINT (-36.4134 -30.3621)
 *   name: Martynov
 *   diameter: 61.0
 * mars.3
 *   geom: POINT (-2.75999999999999 -86.876)
 *   name: Australe Mensa
 *   diameter: 172.0
 * mars.4
 *   geom: POINT (-65 -9.885)
 *   name: Ophir
 *   diameter: 0.0
 * </pre>
 */
public class GetFeatureIntegrationTest extends AbstractAclWFSIntegrationTest {

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {
        addUser("iau", "pwd", null, List.of("ROLE_IAU"));
    }

    @Before
    public void setUpAcl() throws IOException {
        support.addRule(10, DENY, null, null, null, null, null, null);
    }

    @Test
    public void testGetAsAnonymous() throws Exception {
        Document response = getAsDOM("wfs?request=GetFeature&typename=iau:MarsPoi&version=1.1.0&service=wfs");
        assertExceptionReport(response, "Feature type iau:MarsPoi unknown");
    }

    @Test
    public void testGetAsAdmin() throws Exception {
        loginAsAdmin();
        Document response = getAsDOM("wfs?request=GetFeature&typename=iau:MarsPoi&version=1.1.0&service=wfs");
        assertXpathEvaluatesTo("4", "count(//wfs:FeatureCollection/gml:featureMembers/iau:MarsPoi)", response);
    }

    @Test
    public void testGetWithCqlFilterRead() throws Exception {
        Rule allowRule = support.addRule(1, ALLOW, null, "ROLE_IAU", null, null, "iau", "MarsPoi");
        support.setCqlReadFilter(allowRule, "diameter > 0");

        login("iau", "pwd", "ROLE_IAU");

        // With read filter "diameter > 0", should only see 3 features (all except Ophir which has diameter=0)
        Document response = getAsDOM("wfs?request=GetFeature&typename=iau:MarsPoi&version=1.1.0&service=wfs");
        assertXpathEvaluatesTo("3", "count(//wfs:FeatureCollection/gml:featureMembers/iau:MarsPoi)", response);

        // Update rule to filter "diameter = 0", should only see Ophir
        support.setCqlReadFilter(allowRule, "diameter = 0");
        response = getAsDOM("wfs?request=GetFeature&typename=iau:MarsPoi&version=1.1.0&service=wfs");
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection/gml:featureMembers/iau:MarsPoi)", response);
        // mind the name property is encoded as gml:name, not iau:name
        assertXpathEvaluatesTo("Ophir", "//wfs:FeatureCollection/gml:featureMembers/iau:MarsPoi[1]/gml:name", response);
    }

    @Test
    public void testPostAsAnonymous() throws Exception {
        String xml =
                """
                <wfs:GetFeature service="WFS" version="1.1.0"
                  xmlns:iau="http://geoserver.org/iau"
                  xmlns:wfs="http://www.opengis.net/wfs">
                <wfs:Query typeName="iau:MarsPoi"/>
                </wfs:GetFeature>
                """;

        Document response = postAsDOM("wfs", xml);
        // print(response);
        assertExceptionReport(response, "Could not locate {http://geoserver.org/iau}MarsPoi in catalog.");
    }

    @Test
    public void testPostAsAdmin() throws Exception {
        loginAsAdmin();

        String xml =
                """
                <wfs:GetFeature service="WFS" version="1.1.0"
                  xmlns:iau="http://geoserver.org/iau"
                  xmlns:wfs="http://www.opengis.net/wfs">
                <wfs:Query typeName="iau:MarsPoi"/>
                </wfs:GetFeature>
                """;

        Document response = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", response.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("4", "count(//wfs:FeatureCollection/gml:featureMembers/iau:MarsPoi)", response);
    }

    @Test
    public void testPostWithCqlFilterRead() throws Exception {
        Rule allowRule = support.addRule(1, ALLOW, null, "ROLE_IAU", null, null, "iau", "MarsPoi");
        support.setCqlReadFilter(allowRule, "diameter > 0");

        login("iau", "pwd", "ROLE_IAU");

        String xml =
                """
                <wfs:GetFeature service="WFS" version="1.1.0"
                  xmlns:iau="http://geoserver.org/iau"
                  xmlns:wfs="http://www.opengis.net/wfs">
                <wfs:Query typeName="iau:MarsPoi"/>
                </wfs:GetFeature>
                """;

        // With read filter "diameter > 0", should only see 3 features (all except Ophir which has diameter=0)
        Document response = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", response.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("3", "count(//wfs:FeatureCollection/gml:featureMembers/iau:MarsPoi)", response);

        // Update rule to filter "diameter = 0", should only see Ophir
        support.setCqlReadFilter(allowRule, "diameter = 0");
        response = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", response.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//wfs:FeatureCollection/gml:featureMembers/iau:MarsPoi)", response);
        // mind the name property is encoded as gml:name, not iau:name
        assertXpathEvaluatesTo("Ophir", "//wfs:FeatureCollection/gml:featureMembers/iau:MarsPoi[1]/gml:name", response);
    }
}
