/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.geoserver.ows;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockServletContext;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Ian Schneider <ischneider@boundlessgeo.com>
 */
public class FilePublisherTest {

    static FilePublisher publisher;
    static List<String[]> paths = new ArrayList<String[]>();

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
        publisher.setServletContext(new MockServletContext());
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
        FileOutputStream fout = new FileOutputStream(file);
        fout.write(fname.getBytes("UTF-8"));
        fout.close();
        return path;
    }

    @AfterClass
    public static void destroy() {

    }

    private MockHttpServletResponse request(String[] path, String modifiedSince) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        StringBuilder b = new StringBuilder("/geoserver");
        for (int i = 0; i < path.length; i++) {
            b.append('/').append(path[i]);
        }
        String uri = URLEncoder.encode(b.toString(), "UTF-8");
        request.setRequestURI(uri);
        if (modifiedSince != null) {
            request.setHeader("If-Modified-Since", modifiedSince);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        publisher.handleRequest(request, response);
        return response;
    }

    @Test
    public void testEncoding() throws Exception {
        for (String[] path: paths) {
            MockHttpServletResponse response = request(path, null);
            assertEquals(Arrays.toString(path), 200, response.getErrorCode());
            assertEquals(path[path.length - 1], response.getOutputStreamContent());
        }
    }

    @Test
    public void testLastModified() throws Exception {
        for (String[] path: paths) {
            MockHttpServletResponse response = request(path, null);

            String lastModified = response.getHeader("Last-Modified");
            assertNotNull(lastModified);
            response = request(path, lastModified);
            assertEquals(304, response.getStatusCode());

            long timeStamp = AbstractURLPublisher.lastModified(lastModified) + 10000;
            response = request(path, AbstractURLPublisher.lastModified(timeStamp));
            assertEquals(304, response.getStatusCode());

            timeStamp -= 20000;
            response = request(path, AbstractURLPublisher.lastModified(timeStamp));
            assertEquals(200, response.getErrorCode());
            assertEquals(path[path.length - 1], response.getOutputStreamContent());
        }
    }

}
