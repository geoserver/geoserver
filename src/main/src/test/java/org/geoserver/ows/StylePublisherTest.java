/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

/** @author Ian Schneider <ischneider@boundlessgeo.com> */
public class StylePublisherTest extends GeoServerSystemTestSupport {

    static StylePublisher publisher;

    static MockServletContext context;

    static List<String[]> paths = new ArrayList<>();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {

        Catalog catalog = getCatalog();
        publisher = new StylePublisher(catalog);
        context = new MockServletContext();
        publisher.setServletContext(context);
        GeoServerResourceLoader resourceLoader = getResourceLoader();

        // add style - global
        resourceLoader.copyFromClassPath(
                "org/geoserver/ows/smileyface.png", "styles/smileyface.png");
        paths.add(new String[] {"styles", "smileyface.png"});

        // add style - global in subdirectory
        resourceLoader.copyFromClassPath(
                "org/geoserver/ows/smileyface.png", "styles/icons/smileyface.png");
        paths.add(new String[] {"styles", "icons", "smileyface.png"});

        // add style - workspaced
        resourceLoader.copyFromClassPath(
                "org/geoserver/ows/house.svg", "workspaces/cite/styles/house.svg");
        paths.add(new String[] {"styles", "cite", "house.svg"});

        // add style - workspaced in subdirectory
        resourceLoader.copyFromClassPath(
                "org/geoserver/ows/house.svg", "workspaces/cite/styles/icons/house.svg");
        paths.add(new String[] {"styles", "cite", "icons", "house.svg"});

        // add style - workspaced style with global image
        paths.add(new String[] {"styles", "cite", "smileyface.png"});

        // add style - workspaced style with global image in subdirectory
        paths.add(new String[] {"styles", "cite", "icons", "smileyface.png"});

        // testing over-riding global image with workspaced image
        resourceLoader.copyFromClassPath("org/geoserver/ows/smileyface.png", "styles/override.png");
        resourceLoader.copyFromClassPath(
                "org/geoserver/ows/grass_fill.png", "workspaces/cite/styles/override.png");

        // testing over-riding global image with workspaced image in subdirectory
        resourceLoader.copyFromClassPath(
                "org/geoserver/ows/smileyface.png", "styles/icons/override.png");
        resourceLoader.copyFromClassPath(
                "org/geoserver/ows/grass_fill.png", "workspaces/cite/styles/icons/override.png");

        resourceLoader.createFile("styles/test.foo");
        resourceLoader.createFile("styles/test.bar");
    }

    private MockHttpServletResponse request(String[] path, String modifiedSince) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setMethod("GET");
        StringBuilder b = new StringBuilder("/geoserver");
        for (String s : path) {
            b.append('/').append(s);
        }
        String uri = URLEncoder.encode(b.toString(), "UTF-8");
        request.setRequestURI(uri);
        if (modifiedSince != null) {
            request.addHeader("If-Modified-Since", modifiedSince);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        publisher.handleRequest(request, response);
        return response;
    }

    @Test
    public void testBlockXML() throws Exception {
        // test that access to styles' XML configuration files is blocked
        String[] path1 = {"styles/Default.sld"};
        MockHttpServletResponse response1 = request(path1, null);
        assertEquals(200, response1.getStatus());
        String[] path2 = {"styles/Default.xml"};
        MockHttpServletResponse response2 = request(path2, null);
        assertEquals(404, response2.getStatus());
    }

    @Test
    public void testDefaultContentType() throws Exception {
        // test that the application/octect-stream content type is used if
        // the servlet context doesn't contain a mapping for the mime type.
        String[] path = {"styles/test.bar"};
        MockHttpServletResponse response = request(path, null);
        assertEquals(200, response.getStatus());
        assertThat(
                response.getHeader("Content-Type"),
                startsWith(MediaType.APPLICATION_OCTET_STREAM_VALUE));
    }

    @Test
    public void testForceDownload() throws Exception {
        // test that style resources with certain mime types have a Content-Disposition
        // to force the web browser to download the file
        String[] path = {"styles/test.foo"};
        context.addMimeType("foo", MediaType.TEXT_XML);
        MockHttpServletResponse response1 = request(path, null);
        assertEquals(200, response1.getStatus());
        assertThat(response1.getHeader("Content-Type"), startsWith(MediaType.TEXT_XML_VALUE));
        assertEquals(
                "attachment; filename=\"test.foo\"", response1.getHeader("Content-Disposition"));

        context.addMimeType("foo", MediaType.APPLICATION_XML);
        MockHttpServletResponse response2 = request(path, null);
        assertEquals(200, response2.getStatus());
        assertThat(
                response2.getHeader("Content-Type"), startsWith(MediaType.APPLICATION_XML_VALUE));
        assertEquals(
                "attachment; filename=\"test.foo\"", response2.getHeader("Content-Disposition"));

        context.addMimeType("foo", MediaType.TEXT_HTML);
        MockHttpServletResponse response3 = request(path, null);
        assertEquals(200, response3.getStatus());
        assertThat(response3.getHeader("Content-Type"), startsWith(MediaType.TEXT_HTML_VALUE));
        assertEquals(
                "attachment; filename=\"test.foo\"", response3.getHeader("Content-Disposition"));

        context.addMimeType("foo", MediaType.valueOf("image/svg+xml"));
        MockHttpServletResponse response4 = request(path, null);
        assertEquals(200, response4.getStatus());
        assertThat(response4.getHeader("Content-Type"), startsWith("image/svg+xml"));
        assertEquals(
                "attachment; filename=\"test.foo\"", response4.getHeader("Content-Disposition"));
    }

    @Test
    public void testEncoding() throws Exception {
        for (String[] path : paths) {
            MockHttpServletResponse response = request(path, null);
            assertEquals(Arrays.toString(path), 200, response.getStatus());

            assertArrayEquals(
                    IOUtils.toByteArray(
                            this.getClass()
                                    .getClassLoader()
                                    .getResourceAsStream(
                                            "org/geoserver/ows/" + path[path.length - 1])),
                    response.getContentAsByteArray());
        }
    }

    @Test
    public void testOverride() throws Exception {
        String[] path = {"styles", "override.png"};
        MockHttpServletResponse response = request(path, null);
        assertEquals(Arrays.toString(path), 200, response.getStatus());

        assertArrayEquals(
                IOUtils.toByteArray(
                        this.getClass()
                                .getClassLoader()
                                .getResourceAsStream("org/geoserver/ows/smileyface.png")),
                response.getContentAsByteArray());

        path = new String[] {"styles", "cite", "override.png"};
        response = request(path, null);
        assertEquals(Arrays.toString(path), 200, response.getStatus());

        assertArrayEquals(
                IOUtils.toByteArray(
                        this.getClass()
                                .getClassLoader()
                                .getResourceAsStream("org/geoserver/ows/grass_fill.png")),
                response.getContentAsByteArray());

        path = new String[] {"styles", "icons", "override.png"};
        response = request(path, null);
        assertEquals(Arrays.toString(path), 200, response.getStatus());

        assertArrayEquals(
                IOUtils.toByteArray(
                        this.getClass()
                                .getClassLoader()
                                .getResourceAsStream("org/geoserver/ows/smileyface.png")),
                response.getContentAsByteArray());

        path = new String[] {"styles", "cite", "icons", "override.png"};
        response = request(path, null);
        assertEquals(Arrays.toString(path), 200, response.getStatus());

        assertArrayEquals(
                IOUtils.toByteArray(
                        this.getClass()
                                .getClassLoader()
                                .getResourceAsStream("org/geoserver/ows/grass_fill.png")),
                response.getContentAsByteArray());
    }

    @Test
    public void testLastModified() throws Exception {
        for (String[] path : paths) {
            MockHttpServletResponse response = request(path, null);

            String lastModified = response.getHeader("Last-Modified");
            assertNotNull(lastModified);
            response = request(path, lastModified);
            assertEquals(304, response.getStatus());

            long timeStamp = AbstractURLPublisher.lastModified(lastModified) + 10000;
            response = request(path, AbstractURLPublisher.lastModified(timeStamp));
            assertEquals(304, response.getStatus());

            timeStamp -= 20000;
            response = request(path, AbstractURLPublisher.lastModified(timeStamp));
            assertEquals(200, response.getStatus());
            assertArrayEquals(
                    IOUtils.toByteArray(
                            this.getClass()
                                    .getClassLoader()
                                    .getResourceAsStream(
                                            "org/geoserver/ows/" + path[path.length - 1])),
                    response.getContentAsByteArray());
        }
    }
}
