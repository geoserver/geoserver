/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.rules.TemporaryFolder;

/**
 * Test handling of file wrapper (Files.ResourceAdaptor) which works with absolute paths internally.
 *
 * <p>This ResourceAdaptor is intended to allow GeoServer to work with absolute paths outside of the data direct. As a
 * result stores an absolute file internally.
 */
public class FileWrapperResourceTheoryTest extends ResourceTheoryTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @DataPoints
    public static String[] getTestPaths() {
        return new String[] {"FileA", "FileB", "DirC", "DirE", "DirC/FileD", "UndefF", "DirC/UndefF", "DirE/UndefF"};
    }

    /**
     * Resource, relative to temporary folder above.
     *
     * @param path resource path
     * @return Resource
     * @throws Exception
     */
    @Override
    protected Resource getResource(String path) throws Exception {
        File file = FilePaths.toFile(folder.getRoot(), path);
        return Files.asResource(file);
    }

    @Before
    public void setUp() throws Exception {
        folder.newFile("FileA");
        folder.newFile("FileB");
        File c = folder.newFolder("DirC");
        (new File(c, "FileD")).createNewFile();
        folder.newFolder("DirE");
    }

    @Override
    protected Resource getDirectory() {
        try {
            return Files.asResource(folder.newFolder("NonTestDir"));
        } catch (IOException e) {
            fail();
            return null;
        }
    }

    @Override
    protected Resource getResource() {
        try {
            return Files.asResource(folder.newFile("NonTestFile"));
        } catch (IOException e) {
            fail();
            return null;
        }
    }

    @Override
    protected Resource getUndefined() {
        return Files.asResource(new File(folder.getRoot(), "NonTestUndef"));
    }

    // paths for file wrapper are special so ignore this test
    @Override
    @Ignore
    public void theoryHaveSamePath(String path) throws Exception {}
}
