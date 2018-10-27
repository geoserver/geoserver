/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.xml.v1_0;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import net.opengis.ows10.ExceptionReportType;
import net.opengis.ows10.ExceptionType;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.test.XMLTestSupport;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ExceptionReportBindingTest extends XMLTestSupport {

    @Override
    protected Configuration createConfiguration() {
        return new OWSConfiguration();
    }

    Document dom(String xml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes()));
    }

    public void testParseServiceException() throws Exception {
        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<ows:ExceptionReport version=\"1.0.0\"\n"
                        + "  xsi:schemaLocation=\"http://www.opengis.net/ows http://demo.opengeo.org/geoserver/schemas/ows/1.0.0/owsExceptionReport.xsd\"\n"
                        + "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ows=\"http://www.opengis.net/ows\">\n"
                        + "  <ows:Exception exceptionCode=\"InvalidParameterValue\" locator=\"service\">\n"
                        + "    <ows:ExceptionText>No service: ( madeUp )</ows:ExceptionText>\n"
                        + "  </ows:Exception>\n"
                        + "</ows:ExceptionReport>\n"
                        + "";

        document = dom(xml);
        Object result = parse(OWS.EXCEPTIONREPORT);

        assertNotNull(result);
        assertTrue(result instanceof ExceptionReportType);
        ExceptionReportType er = (ExceptionReportType) result;

        assertEquals("1.0.0", er.getVersion());
        assertEquals(1, er.getException().size());
        ExceptionType ex = (ExceptionType) er.getException().get(0);
        assertEquals("InvalidParameterValue", ex.getExceptionCode());
        assertEquals("service", ex.getLocator());
        assertEquals(1, ex.getExceptionText().size());
        assertEquals("No service: ( madeUp )", ex.getExceptionText().get(0));
    }
}
