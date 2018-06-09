/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;
import org.w3c.dom.Document;

public class ExtendedOperatorTest extends WFS20TestSupport {

    @Test
    public void testInvokeExtendedOperator() throws Exception {

        String xml =
                "<wfs:GetFeature service='WFS' version='2.0.0' "
                        + "xmlns:wfs='http://www.opengis.net/wfs/2.0' "
                        + "xmlns:fes='http://www.opengis.net/fes/2.0' "
                        + "xmlns:foo='http://foo.org'> "
                        + "<wfs:Query typeNames='sf:PrimitiveGeoFeature'> "
                        + "  <fes:Filter>"
                        + "   <foo:strMatches>"
                        + "     <fes:ValueReference>name</fes:ValueReference>"
                        + "     <fes:Literal>name-f002</fes:Literal>"
                        + "   </foo:strMatches>"
                        + "  </fes:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//sf:PrimitiveGeoFeature)", doc);
        XMLAssert.assertXpathExists("//sf:PrimitiveGeoFeature/gml:name[text()='name-f002']", doc);
    }
}
