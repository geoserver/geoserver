package org.geoserver.wfs;

import org.geoserver.util.EntityResolverProvider;
import org.geotools.util.PreventLocalEntityResolver;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

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
        // enable entity parsing
        System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "true");
        String output = string(post("wfs", WFS_2_0_0_REQUEST));
        // the server tried to read a file on local file system
        assertTrue(output.indexOf("xml request is most probably not compliant to GetFeature element") > -1);

        // disable entity parsing
        System.setProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED, "false");
        output = string(post("wfs", WFS_2_0_0_REQUEST));
        assertTrue(output.indexOf("Request parsing failed") > -1);
        assertTrue(output.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));

        // set default (entity parsing disabled);
        System.clearProperty(EntityResolverProvider.ENTITY_RESOLUTION_UNRESTRICTED);
        output = string(post("wfs", WFS_2_0_0_REQUEST));
        assertTrue(output.indexOf("Request parsing failed") > -1);
        assertTrue(output.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
    }
}
