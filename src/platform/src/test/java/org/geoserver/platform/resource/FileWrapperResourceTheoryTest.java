package org.geoserver.platform.resource;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.rules.TemporaryFolder;

public class FileWrapperResourceTheoryTest extends ResourceTheoryTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @DataPoints
    public static String[] testPaths() {
        return new String[] { "FileA", "FileB", "DirC/FileD", "UndefF",
                "DirC/UndefF", "DirE/UndefF" };
    }

    @Override
    protected Resource getResource(String path) throws Exception {
        final File file = Paths.toFile(folder.getRoot(), path);
        assumeFalse(file.isDirectory());
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
        return Files.asResource(new File(folder.getRoot(),"NonTestUndef"));
    }

    // Directories are not allowed for this class, so ignore the tests.
    @Override @Ignore
    public void theoryDirectoriesHaveNoIstreams(String path) throws Exception {
        
    }

    @Override @Ignore
    public void theoryDirectoriesHaveNoOstream(String path) throws Exception {
        
    }

    @Override @Ignore
    public void theoryDirectoriesHaveChildren(String path) throws Exception {
        
    }

    @Override @Ignore
    public void theoryChildrenKnowTheirParents(String path) throws Exception {
        
    }

    @Override @Ignore
    public void theoryParentsKnowTheirChildren(String path) throws Exception {
        
    }

    @Override @Ignore
    public void theoryParentIsDirectory(String path) throws Exception {
        
    }

    @Override @Ignore
    public void theoryHaveDir(String path) throws Exception {
        
    }

    @Override @Ignore
    public void theoryDirectoriesHaveFileWithSameNamedChildren(String path)
            throws Exception {
        
    }

    @Override @Ignore
    public void theoryAddingFileToDirectoryAddsResource(String path)
            throws Exception {
        
    }
    
    // paths for file wrapper are special so ignore this test
    @Override @Ignore
    public void theoryHaveSamePath(String path)
            throws Exception {
        
    }
    
}
