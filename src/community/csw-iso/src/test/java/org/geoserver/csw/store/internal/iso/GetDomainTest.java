/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal.iso;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import org.junit.Test;
import org.w3c.dom.Document;

public class GetDomainTest extends MDTestSupport {

    @Test
    public void testGetDomain() throws Exception {
        Document dom =
                getAsDOM("csw?service=csw&version=2.0.2&request=GetDomain&propertyName=Title");
        print(dom);

        assertXpathEvaluatesTo(
                "Title", "/csw:GetDomainResponse/csw:DomainValues/csw:PropertyName", dom);
        assertXpathEvaluatesTo("29", "count(//csw:Value)", dom);
        assertXpathExists("//csw:Value[.='AggregateGeoFeature']", dom);
        assertXpathExists("//csw:Value[.='BasicPolygons']", dom);
        assertXpathExists("//csw:Value[.='Bridges']", dom);
    }
}
