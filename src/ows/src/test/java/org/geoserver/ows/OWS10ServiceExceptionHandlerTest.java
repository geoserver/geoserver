/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import javax.xml.parsers.DocumentBuilderFactory;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class OWS10ServiceExceptionHandlerTest {

    private static OWS10ServiceExceptionHandler handler;
    private static MockHttpServletRequest request;
    private static MockHttpServletResponse response;
    private static Request requestInfo;

    private static final String XML_TYPE_TEXT = "text/xml";

    private XpathEngine xpath;

    @BeforeClass
    public static void setupClass()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        // Playing with System.Properties and Static boolean fields can raises issues
        // when running Junit tests via Maven, due to initialization orders.
        // So let's change the fields via reflections for these tests
        Field field = OWS10ServiceExceptionHandler.class.getDeclaredField("CONTENT_TYPE");
        field.setAccessible(true);
        field.set(null, XML_TYPE_TEXT);
    }

    @AfterClass
    public static void teardownClass() {
        System.clearProperty("ows10.exception.xml.responsetype");
    }

    @Before
    public void setUp() throws Exception {
        HelloWorld helloWorld = new HelloWorld();
        Service service = new Service("hello", helloWorld, new Version("1.0.0"), Collections.singletonList("hello"));

        request = new MockHttpServletRequest() {
            @Override
            public int getServerPort() {
                return 8080;
            }
        };

        request.setScheme("http");
        request.setServerName("localhost");

        request.setContextPath("geoserver");

        response = new MockHttpServletResponse();

        handler = new OWS10ServiceExceptionHandler();

        requestInfo = new Request();
        requestInfo.setHttpRequest(request);
        requestInfo.setHttpResponse(response);
        requestInfo.setService(service.getId());
        requestInfo.setVersion(service.getVersion().toString());
        xpath = XMLUnit.newXpathEngine();
        xpath.setNamespaceContext(
                new SimpleNamespaceContext(Collections.singletonMap("ows", "http://www.opengis.net/ows")));
    }

    @Test
    public void testHandleServiceException() throws Exception {
        ServiceException exception = new ServiceException("hello service exception");
        exception.setCode("helloCode");
        exception.setLocator("helloLocator");
        exception.getExceptionText().add("helloText");
        handler.handleServiceException(exception, requestInfo);

        InputStream input =
                new ByteArrayInputStream(response.getContentAsString().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);
        assertEquals("ows:ExceptionReport", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testHandleServiceExceptionEncoding() throws Exception {
        String message = "foo & <foo> \"foo's\"";

        ServiceException exception = new ServiceException(message);
        exception.setLocator("test-locator");

        handler.handleServiceException(exception, requestInfo);

        InputStream input =
                new ByteArrayInputStream(response.getContentAsString().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);

        assertEquals(
                "round-tripped through character entities",
                message,
                xpath.evaluate("ows:ExceptionReport/ows:Exception/ows:ExceptionText", doc));
    }

    @Test
    public void testHandleServiceExceptionEncodingMore() throws Exception {
        String message1 = "foo & <foo> \"foo's\"";
        String message2 = "a \"different\" <message>";

        ServiceException exception = new ServiceException(message1);
        exception.setLocator("test-locator");
        exception.getExceptionText().add(message2);

        handler.handleServiceException(exception, requestInfo);

        InputStream input =
                new ByteArrayInputStream(response.getContentAsString().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);

        String message = message1 + "\n" + message2;
        assertEquals(
                "round-tripped through character entities",
                message,
                xpath.evaluate("ows:ExceptionReport/ows:Exception/ows:ExceptionText", doc));
    }

    @Test
    public void testHandleServiceExceptionCauses() throws Exception {
        // create a stack of three exceptions
        IllegalArgumentException illegalArgument = new IllegalArgumentException("Illegal argument here");
        IOException ioException = new IOException("I/O exception here");
        ioException.initCause(illegalArgument);
        ServiceException serviceException = new ServiceException("hello service exception");
        serviceException.setCode("helloCode");
        serviceException.setLocator("helloLocator");
        serviceException.getExceptionText().add("helloText");
        serviceException.initCause(ioException);
        handler.handleServiceException(serviceException, requestInfo);

        InputStream input =
                new ByteArrayInputStream(response.getContentAsString().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);
        // normalise whitespace
        String exceptionText = xpath.evaluate("ows:ExceptionReport/ows:Exception/ows:ExceptionText", doc)
                .replaceAll("\\s+", " ");
        assertNotEquals(exceptionText.indexOf(illegalArgument.getMessage()), -1);
        assertNotEquals(exceptionText.indexOf(ioException.getMessage()), -1);
        assertNotEquals(exceptionText.indexOf(serviceException.getMessage()), -1);
    }

    @Test
    public void testHandleServiceExceptionNullMessages() throws Exception {
        // create a stack of three exceptions
        NullPointerException npe = new NullPointerException();
        ServiceException serviceException = new ServiceException("hello service exception");
        serviceException.setCode("helloCode");
        serviceException.setLocator("helloLocator");
        serviceException.getExceptionText().add("NullPointerException");
        serviceException.initCause(npe);
        handler.handleServiceException(serviceException, requestInfo);

        InputStream input =
                new ByteArrayInputStream(response.getContentAsString().getBytes());

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        Document doc = docBuilderFactory.newDocumentBuilder().parse(input);
        // normalise whitespace
        String exceptionText = xpath.evaluate("ows:ExceptionReport/ows:Exception/ows:ExceptionText", doc)
                .replaceAll("\\s+", " ");
        // used to contain an extra " null" at the end
        assertEquals("hello service exception NullPointerException", exceptionText);
    }

    @Test
    public void exceptionType() throws Exception {
        String message1 = "foo & <foo> \"foo's\"";
        String message2 = "a \"different\" <message>";

        ServiceException exception = new ServiceException(message1);
        exception.setLocator("test-locator");
        exception.getExceptionText().add(message2);

        handler.handleServiceException(exception, requestInfo);
        assertEquals(XML_TYPE_TEXT, response.getContentType());
    }
}
