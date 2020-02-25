/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class OWS11ServiceExceptionHandlerTest {

    private static OWS11ServiceExceptionHandler handler;
    private static MockHttpServletRequest request;
    private static MockHttpServletResponse response;
    private static Request requestInfo;

    private static final String XML_TYPE_TEXT = "text/xml";

    @BeforeClass
    public static void setupClass() {
        System.setProperty("ows11.exception.xml.responsetype", XML_TYPE_TEXT);
    }

    @AfterClass
    public static void teardownClass() {
        System.clearProperty("ows11.exception.xml.responsetype");
    }

    @Before
    public void setUp() throws Exception {
        HelloWorld helloWorld = new HelloWorld();
        Service service =
                new Service(
                        "hello",
                        helloWorld,
                        new Version("1.1.1"),
                        Collections.singletonList("hello"));

        request =
                new MockHttpServletRequest() {
                    public int getServerPort() {
                        return 8080;
                    }
                };

        request.setScheme("http");
        request.setServerName("localhost");
        request.setContextPath("geoserver");
        response = new MockHttpServletResponse();
        handler = new OWS11ServiceExceptionHandler();

        requestInfo = new Request();
        requestInfo.setHttpRequest(request);
        requestInfo.setHttpResponse(response);
        requestInfo.setService(service.getId());
        requestInfo.setVersion(service.getVersion().toString());
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
