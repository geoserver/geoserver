/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertTrue;

import org.geoserver.util.EntityResolverProvider;
import org.junit.Test;
import org.w3c.dom.Document;

public class ExternalEntitiesTest extends WFSTestSupport {

    private static final String WFS_2_0_0_REQUEST =
            """
            <?xml version="1.0" ?>
            <!DOCTYPE wfs:GetFeature [
            <!ELEMENT wfs:GetFeature (wfs:Query*)>
            <!ATTLIST wfs:GetFeature
                            service   CDATA #FIXED "WFS"
                            version   CDATA #FIXED "2.0.0"
                            outputFormat CDATA #FIXED "application/gml+xml; version=3.2"
                    xmlns:wfs CDATA #FIXED "http://www.opengis.net/wfs"
                            xmlns:ogc CDATA #FIXED "http://www.opengis.net/ogc"
                            xmlns:fes CDATA #FIXED "http://www.opengis.net/fes/2.0">
            <!ELEMENT wfs:Query (wfs:PropertyName*,ogc:Filter?)>
            <!ATTLIST wfs:Query typeName CDATA #FIXED "cdf:Fifteen">
            <!ELEMENT wfs:PropertyName (#PCDATA) >
            <!ELEMENT ogc:Filter (fes:ResourceId*)>
            <!ELEMENT fes:ResourceId EMPTY>
            <!ATTLIST fes:ResourceId rid CDATA #FIXED "states.3">

            <!ENTITY passwd  SYSTEM "FILE:///thisfiledoesnotexist?.XSD">
            ]>
            <wfs:GetFeature service="WFS" version="2.0.0" outputFormat="application/gml+xml; version=3.2"
                    xmlns:wfs="http://www.opengis.net/wfs/2.0"
                    xmlns:fes="http://www.opengis.net/fes/2.0">
                    <wfs:Query typeName="cdf:Fifteen">
                            <wfs:PropertyName>&passwd;</wfs:PropertyName>
                            <fes:Filter>
                                    <fes:ResourceId rid="states.3"/>
                            </fes:Filter>
                    </wfs:Query>
            </wfs:GetFeature>""";

    @Test
    public void testWfs2_0() throws Exception {
        Document dom;
        String message;
        System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "true");
        try {
            // enable entity parsing: server tries to read file on local file system
            dom = postAsDOM("wfs", WFS_2_0_0_REQUEST);
            message = checkOws11Exception(dom, "2.0.0", null, null);
            assertTrue(
                    "not compliant to GetFeature element",
                    message.contains("xml request is most probably not compliant to GetFeature element"));

            // disable entity parsing: DOCTYPE must be rejected
            System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "false");
            dom = postAsDOM("wfs", WFS_2_0_0_REQUEST);
            message = checkOws11Exception(dom, "2.0.0", null, null);
            assertTrue(message.contains("DOCTYPE"));
        } finally {
            System.clearProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED);
        }
        // default (entity parsing disabled): DOCTYPE must be rejected
        dom = postAsDOM("wfs", WFS_2_0_0_REQUEST);
        message = checkOws11Exception(dom, "2.0.0", null, null);
        assertTrue(message.contains("DOCTYPE"));
    }
}
