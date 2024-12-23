/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.geoserver.ows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

/** @author Ian Schneider <ischneider@boundlessgeo.com> */
public class FilePublisherTest {

    static FilePublisher publisher;

    static MockServletContext context;

    static List<String[]> paths = new ArrayList<>();

    @BeforeClass
    public static void create() throws Exception {
        File tmp = File.createTempFile("xyz", "123");
        tmp.delete();
        tmp.mkdirs();
        tmp.deleteOnExit();

        paths.add(create(tmp, "with space", "ascii"));
        paths.add(create(tmp, "with space", "làtîn"));

        GeoServerResourceLoader loader = new GeoServerResourceLoader(tmp);
        publisher = new FilePublisher(loader);
        context = new MockServletContext();
        publisher.setServletContext(context);

        Files.write(loader.createFile("index.html").toPath(), "BAD".getBytes());
        loader.createFile("www/test.foo");
        loader.createFile("www/test.bar");
    }

    static String[] create(File parent, String... path) throws Exception {
        for (int i = 0; i < path.length - 1; i++) {
            parent = new File(parent, path[i]);
            parent.deleteOnExit();
        }
        parent.mkdirs();
        String fname = path[path.length - 1];
        File file = new File(parent, fname);
        file.deleteOnExit();
        try (FileOutputStream fout = new FileOutputStream(file)) {
            fout.write(fname.getBytes(StandardCharsets.UTF_8));
        }
        return path;
    }

    @After
    public void resetProperty() {
        System.clearProperty(FilePublisher.DISABLE_STATIC_WEB_FILES);
    }

    @AfterClass
    public static void destroy() {}

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
    public void testEncoding() throws Exception {
        for (String[] path : paths) {
            MockHttpServletResponse response = request(path, null);
            assertEquals(Arrays.toString(path), 200, response.getStatus());
            assertEquals(path[path.length - 1], response.getContentAsString());
        }
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
            assertEquals(path[path.length - 1], response.getContentAsString());
        }
    }

    @Test
    public void testMimeDefault() throws Exception {
        // test that the application/octect-stream content type is used if
        // the servlet context doesn't contain a mapping for the mime type.
        doTestTypeAndDisposition("www/", "test.bar", null, MediaType.APPLICATION_OCTET_STREAM_VALUE, "inline");
    }

    @Test
    public void testMimeTextXml() throws Exception {
        // test that the text/xml content type has a Content-Disposition
        // to force the web browser to download the file
        doTestTypeAndDisposition("www/", "test.foo", MediaType.TEXT_XML, MediaType.TEXT_XML_VALUE, "attachment");
    }

    @Test
    public void testMimeApplicationXml() throws Exception {
        // test that the application/xml content type has a Content-Disposition
        // to force the web browser to download the file
        doTestTypeAndDisposition(
                "www/", "test.foo", MediaType.APPLICATION_XML, MediaType.APPLICATION_XML_VALUE, "attachment");
    }

    @Test
    public void testMimeImageSvgXml() throws Exception {
        // test that the image/svg+xml content type has a Content-Disposition
        // to force the web browser to download the file
        doTestTypeAndDisposition("www/", "test.foo", MediaType.valueOf("image/svg+xml"), "image/svg+xml", "attachment");
    }

    @Test
    public void testMimeTextHtmlPropUndefined() throws Exception {
        // test that the text/html content type is allowed
        System.clearProperty(FilePublisher.DISABLE_STATIC_WEB_FILES);
        doTestTypeAndDisposition("www/", "test.foo", MediaType.TEXT_HTML, MediaType.TEXT_HTML_VALUE, "inline");
    }

    @Test
    public void testMimeTextHtmlPropFalse() throws Exception {
        // test that the text/html content type is allowed
        System.setProperty(FilePublisher.DISABLE_STATIC_WEB_FILES, "false");
        doTestTypeAndDisposition("www/", "test.foo", MediaType.TEXT_HTML, MediaType.TEXT_HTML_VALUE, "inline");
    }

    @Test
    public void testMimeTextHtmlPropTrue() throws Exception {
        // test that the text/html content type is replaced with text/plain
        System.setProperty(FilePublisher.DISABLE_STATIC_WEB_FILES, "true");
        doTestTypeAndDisposition("www/", "test.foo", MediaType.TEXT_HTML, MediaType.TEXT_PLAIN_VALUE, "inline");
    }

    @Test
    public void testMimeApplicationJavascriptPropUndefined() throws Exception {
        // test that the application/javascript content type is allowed
        System.clearProperty(FilePublisher.DISABLE_STATIC_WEB_FILES);
        doTestTypeAndDisposition(
                "www/", "test.foo", MediaType.valueOf("application/javascript"), "application/javascript", "inline");
    }

    @Test
    public void testMimeApplicationJavascriptPropFalse() throws Exception {
        // test that the application/javascript content type is allowed
        System.setProperty(FilePublisher.DISABLE_STATIC_WEB_FILES, "false");
        doTestTypeAndDisposition(
                "www/", "test.foo", MediaType.valueOf("application/javascript"), "application/javascript", "inline");
    }

    @Test
    public void testMimeApplicationJavascriptPropTrue() throws Exception {
        // test that the application/javascript content type is replaced with text/plain
        System.setProperty(FilePublisher.DISABLE_STATIC_WEB_FILES, "true");
        doTestTypeAndDisposition(
                "www/", "test.foo", MediaType.valueOf("application/javascript"), MediaType.TEXT_PLAIN_VALUE, "inline");
    }

    @Test
    public void testMimeWhitelistedHtmlFile() throws Exception {
        // test that the text/html content type is allowed even when property is true
        System.setProperty(FilePublisher.DISABLE_STATIC_WEB_FILES, "true");
        String content =
                doTestTypeAndDisposition("", "index.html", MediaType.TEXT_HTML, MediaType.TEXT_HTML_VALUE, "inline");
        assertEquals("GOOD", content);
    }

    private String doTestTypeAndDisposition(
            String path, String filename, MediaType serverType, String responseType, String dispositionType)
            throws Exception {
        String[] paths = {path + filename};
        if (serverType != null) {
            context.addMimeType("foo", serverType);
        }
        MockHttpServletResponse response = request(paths, null);
        assertEquals(200, response.getStatus());
        assertThat(response.getHeader("Content-Type"), startsWith(responseType));
        assertEquals(dispositionType + "; filename=\"" + filename + "\"", response.getHeader("Content-Disposition"));
        return response.getContentAsString();
    }
}
