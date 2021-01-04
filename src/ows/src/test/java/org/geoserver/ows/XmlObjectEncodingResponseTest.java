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
import net.opengis.ows10.AcceptVersionsType;
import net.opengis.ows10.GetCapabilitiesType;
import net.opengis.ows10.Ows10Factory;
import org.geoserver.ows.xml.v1_0.OWSConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class XmlObjectEncodingResponseTest {

    XmlObjectEncodingResponse response;

    @Before
    public void setUp() throws Exception {
        response =
                new XmlObjectEncodingResponse(
                        GetCapabilitiesType.class, "GetCapabilities", OWSConfiguration.class);
    }

    @Test
    public void testCanHandle() {
        Assert.assertTrue(response.canHandle(null));
    }

    @Test
    public void testGetMimeType() {
        Assert.assertEquals("application/xml", response.getMimeType(null, null));
    }

    @Test
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

        Assert.assertEquals("ows:GetCapabilities", d.getDocumentElement().getNodeName());
        Assert.assertEquals(2, d.getElementsByTagName("ows:Version").getLength());
    }
}
