package org.geoserver.platform.resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.rules.TemporaryFolder;

public class FileSystemResourceTheoryTest extends ResourceTheoryTest {

    FileSystemResourceStore store;
    
    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    
    @DataPoints
    public static String[] testPaths() {
        return new String[]{"FileA","FileB", "DirC", "DirC/FileD", "DirE", "UndefF", "DirC/UndefF", "DirE/UndefF", "DirE/UndefG/UndefH/UndefI"};
    }

    @Override
    protected Resource getResource(String path) throws Exception{
        return store.get(path);
    }
    
    @Before
    public void setUp() throws Exception {
        folder.newFile("FileA");
        folder.newFile("FileB");
        File c = folder.newFolder("DirC");        
        (new File(c, "FileD")).createNewFile();
        folder.newFolder("DirE");
        store = new FileSystemResourceStore(folder.getRoot());
    }
    
    @Test
    public void invalid(){
        try {
            Resource resource = store.get("foo|");
            assertNotNull( resource );
            fail("| invalid");
        }
        catch( IllegalArgumentException expected){            
        }
    }
}
