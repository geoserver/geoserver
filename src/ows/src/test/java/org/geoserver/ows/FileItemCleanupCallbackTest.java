/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import com.google.common.base.Strings;
import java.io.File;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class FileItemCleanupCallbackTest {

    private static final String BOUNDARY = "----1234";

    // temp files are only created for fields that exceed a certain content length
    private static final String FILE_CONTENTS =
            Strings.repeat("1", DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD + 1);

    private static String oldTmpDir;

    @Rule public TemporaryFolder tmpFolder = new TemporaryFolder(new File("target"));

    @BeforeClass
    public static void saveTempDir() {
        oldTmpDir = System.getProperty("java.io.tmpdir");
    }

    @AfterClass
    public static void clearTempDir() {
        if (oldTmpDir == null) {
            System.clearProperty("java.io.tmpdir");
        } else {
            System.setProperty("java.io.tmpdir", oldTmpDir);
        }
    }

    @Before
    public void initTempDir() throws Exception {
        System.setProperty("java.io.tmpdir", tmpFolder.getRoot().getCanonicalPath());
    }

    @Test
    public void testFormDataEmpty() throws Exception {
        assertEmptyDirectory(null, null);
    }

    @Test
    public void testFormDataUsingFile() throws Exception {
        assertEmptyDirectory("\"file1\"; filename=\"foo.txt\"", "\"file2\"; filename=\"bar.txt\"");
    }

    @Test
    public void testFormDataUsingBody() throws Exception {
        assertEmptyDirectory("\"junk\"", "\"body\"");
    }

    @Test
    public void testFormDataWithoutFileOrBody() throws Exception {
        assertEmptyDirectory("\"junk\"", "\"junk\"");
    }

    private void assertEmptyDirectory(String field1, String field2) throws Exception {
        // create the mock HTTP request
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/geoserver/ows");
        request.setContentType("multipart/form-data; boundary=" + BOUNDARY);
        String content = "";
        if (field1 != null) {
            content =
                    "--"
                            + BOUNDARY
                            + "\r\nContent-Disposition: form-data; name="
                            + field1
                            + "\r\n\r\n"
                            + FILE_CONTENTS
                            + "\r\n--"
                            + BOUNDARY
                            + "\r\nContent-Disposition: form-data; name="
                            + field2
                            + "\r\n\r\n"
                            + FILE_CONTENTS
                            + "\r\n--"
                            + BOUNDARY
                            + "--\r\n";
        }
        request.setContent(content.getBytes());

        // init a new dispatcher and dispatch the request
        URL url = getClass().getResource("applicationContext.xml");
        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {
            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            MockHttpServletResponse response = new MockHttpServletResponse();
            dispatcher.handleRequest(request, response);
        }

        // verify that all upload temp files were deleted
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(tmpFolder.getRoot().toPath(), "upload_*.tmp")) {
            stream.forEach(files::add);
        }
        assertThat("Uploaded files were not deleted", files, empty());
    }
}
