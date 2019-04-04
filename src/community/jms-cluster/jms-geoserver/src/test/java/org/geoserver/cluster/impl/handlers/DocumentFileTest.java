/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import com.thoughtworks.xstream.XStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DocumentFileTest {

    private File rootDirectory;
    private FileSystemResourceStore resourceStore;

    @Before
    public void before() throws Exception {
        rootDirectory = Files.createTempDirectory("jsm-test-").toFile();
        resourceStore = new FileSystemResourceStore(rootDirectory);
    }

    @After
    public void after() throws Exception {
        FileUtils.deleteDirectory(rootDirectory);
    }

    @Test
    public void testSerializeDocumentFile() throws Exception {
        // creating a style in data directory
        Resource resource = addResourceToDataDir("styles/style.sld", "some style definition");
        // instantiating a document file representing the style file
        DocumentFile documentFile = new DocumentFile(resource);
        // serialising the file document
        DocumentFileHandlerSPI handler = new DocumentFileHandlerSPI(0, new XStream());
        String result = handler.createHandler().serialize(documentFile);
        // checking the serialization result
        assertThat(result, notNullValue());
        assertThat(
                XMLUnit.compareXML(readResourceFileContent("document_file_1.xml"), result)
                        .similar(),
                is(true));
    }

    /** Creates a resource path in the data directory and write the provided content in it. */
    private Resource addResourceToDataDir(String resourcePath, String resourceContent)
            throws Exception {
        XMLUnit.setIgnoreWhitespace(true);
        File resourceFile = new File(rootDirectory, resourcePath);
        resourceFile.getParentFile().mkdirs();
        Files.write(resourceFile.toPath(), resourceContent.getBytes());
        return resourceStore.get("styles/style.sld");
    }

    /** Helper method that will read the content of a resource file and return it as a String. */
    private String readResourceFileContent(String resourceFileName) throws Exception {
        try (InputStream input =
                        DocumentFileTest.class
                                .getClassLoader()
                                .getResourceAsStream(resourceFileName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
