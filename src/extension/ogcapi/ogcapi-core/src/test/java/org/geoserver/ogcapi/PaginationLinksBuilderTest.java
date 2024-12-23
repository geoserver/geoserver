/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class PaginationLinksBuilderTest {

    public static final String PATH = "service/v1/resource";
    public static final String URL_BASE = "http://localhost/service/v1/resource";

    @BeforeClass
    public static void setupRequestInfo() {
        RequestAttributes attributes = EasyMock.niceMock(RequestAttributes.class);
        RequestContextHolder.setRequestAttributes(attributes);
        APIRequestInfo requestInfo = new APIRequestInfo(
                new MockHttpServletRequest(), new MockHttpServletResponse(), EasyMock.mock(APIDispatcher.class));
        APIRequestInfo.set(requestInfo);
        EasyMock.expect(attributes.getAttribute(APIRequestInfo.KEY, RequestAttributes.SCOPE_REQUEST))
                .andReturn(requestInfo)
                .anyTimes();
        EasyMock.replay(attributes);
    }

    @Test
    public void testFirst() {
        PaginationLinksBuilder builder = new PaginationLinksBuilder(PATH, 0, 5, 5, 250);
        assertNull(builder.getPrevious());
        assertEquals(URL_BASE + "?startIndex=5&limit=5", builder.getNext());
    }

    @Test
    public void testMid() {
        PaginationLinksBuilder builder = new PaginationLinksBuilder(PATH, 5, 5, 5, 250);
        assertEquals(URL_BASE + "?startIndex=0&limit=5", builder.getPrevious());
        assertEquals(URL_BASE + "?startIndex=10&limit=5", builder.getNext());
    }

    @Test
    public void testLast() {
        PaginationLinksBuilder builder = new PaginationLinksBuilder(PATH, 245, 5, 5, 250);
        assertEquals(URL_BASE + "?startIndex=240&limit=5", builder.getPrevious());
        assertNull(builder.getNext());
    }
}
