package org.geoserver.platform.resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ System.class, Files.class})
public class FileSystemResourceStoreTest {

    TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void initTmpFolder() throws IOException {
        folder = new TemporaryFolder();
        folder.create();
    }

    @After
    public void deleteTmpFolder() throws IOException {
        folder.delete();
    }

    @Test
    public void renameDirNamesDifferLinux() throws IOException, InterruptedException {
        PowerMockito.mockStatic(System.class);
        Mockito.when(System.getProperty("os.name")).thenReturn("linux");
        String newName = "DirB";

        attemptRename("DirA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameDirNamesCaseDifferLinux() throws IOException, InterruptedException {
        PowerMockito.mockStatic(System.class);
        Mockito.when(System.getProperty("os.name")).thenReturn("linux");
        String newName = "Dira";

        attemptRename("DirA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }


    @Test
    public void renameDirNamesDifferWindows() throws IOException, InterruptedException {
        PowerMockito.mockStatic(System.class);
        Mockito.when(System.getProperty("os.name")).thenReturn("Windows");
        String newName = "DirB";

        attemptRename("DirA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);

    }

    @Test
    public void renameDirNamesCaseDifferWindows() throws IOException, InterruptedException {
        PowerMockito.mockStatic(System.class);
        Mockito.when(System.getProperty("os.name")).thenReturn("Windows");
        String oldName = "DirA";
        String newName = "Dira";

        attemptRename("DirA", newName);

        assertEquals(oldName, folder.getRoot().list()[0]);
    }

    private void attemptRename(String oldName, String newName) throws IOException {
        File toBeRenamed = folder.newFolder(oldName);
        assertEquals(1, folder.getRoot().list().length);
        FileSystemResourceStore toTest = new FileSystemResourceStore(folder.getRoot());

        toTest.move(oldName, newName);

        assertEquals(1, folder.getRoot().list().length);
    }
}
