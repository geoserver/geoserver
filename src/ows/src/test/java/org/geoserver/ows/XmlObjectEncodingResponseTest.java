/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import junit.framework.TestCase;
import net.opengis.ows10.AcceptVersionsType;
import net.opengis.ows10.GetCapabilitiesType;
import net.opengis.ows10.Ows10Factory;
import org.geoserver.ows.xml.v1_0.OWSConfiguration;
import org.w3c.dom.Document;

public class XmlObjectEncodingResponseTest extends TestCase {

    XmlObjectEncodingResponse response;

    @Override
    protected void setUp() throws Exception {
        response =
                new XmlObjectEncodingResponse(
                        GetCapabilitiesType.class, "GetCapabilities", OWSConfiguration.class);
    }

    public void testCanHandle() {
        assertTrue(response.canHandle(null));
    }

    public void testGetMimeType() {
        assertEquals("application/xml", response.getMimeType(null, null));
    }

    public void testEncode() throws Exception {
        Ows10Factory f = Ows10Factory.eINSTANCE;
        GetCapabilitiesType caps = f.createGetCapabilitiesType();
        AcceptVersionsType versions = f.createAcceptVersionsType();
        caps.setAcceptVersions(versions);

        versions.getVersion().add("1.0.0");
        versions.getVersion().add("1.1.0");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.write(caps, output, null);

        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        TransformerFactory.newInstance()
                .newTransformer()
                .transform(
                        new StreamSource(new ByteArrayInputStream(output.toByteArray())),
                        new DOMResult(d));

        assertEquals("ows:GetCapabilities", d.getDocumentElement().getNodeName());
        assertEquals(2, d.getElementsByTagName("ows:Version").getLength());
    }
}
