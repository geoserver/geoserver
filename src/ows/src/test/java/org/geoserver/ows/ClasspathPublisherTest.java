/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ClasspathPublisherTest {

    @Test
    public void testPathTraversalAnyOS() {
        doTestPathTraversal("/schemas/../META-INF/MANIFEST.MF");
    }

    @Test
    public void testPathTraversalWindowsOnly() {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        doTestPathTraversal("/schemas/..\\META-INF/MANIFEST.MF");
    }

    private static void doTestPathTraversal(String path) {
        ClasspathPublisher publisher = new ClasspathPublisher();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> publisher.handleRequest(request, response));
        assertThat(exception.getMessage(), startsWith("Contains invalid '..' path: "));
    }
}
