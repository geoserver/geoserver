/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ClasspathPublisherTest {

    @Test
    public void testPathTraversal() throws Exception {
        String path = "/schemas/../META-INF/MANIFEST.MF";
        ClasspathPublisher publisher = new ClasspathPublisher();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> publisher.handleRequest(request, response));
        assertEquals("Contains invalid '..' path: " + path, exception.getMessage());
    }
}
