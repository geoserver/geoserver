/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileSystemResourceStoreTest {

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private FileSystemResourceStore store;

    public @Before void before() {
        store = new FileSystemResourceStore(folder.getRoot());
    }

    @Test
    public void renameSameFileName() throws IOException, InterruptedException {
        String sameName = "Filea";

        attemptRenameFile(sameName, sameName);

        assertEquals(sameName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameFileNamesCaseDiffer() throws IOException, InterruptedException {
        String newName = "Filea";

        attemptRenameFile("FileA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameFileNamesDiffer() throws IOException, InterruptedException {
        String newName = "FileB";

        attemptRenameFile("FileA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameSameDirName() throws IOException, InterruptedException {
        String sameName = "Dira";

        attemptRenameDir(sameName, sameName);

        assertEquals(sameName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameDirNamesCaseDiffer() throws IOException, InterruptedException {
        String newName = "Dira";

        attemptRenameDir("DirA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }

    @Test
    public void renameDirNamesDiffer() throws IOException, InterruptedException {
        String newName = "DirB";

        attemptRenameDir("DirA", newName);

        assertEquals(newName, folder.getRoot().list()[0]);
    }

    private void attemptRenameDir(String oldName, String newName) throws IOException {
        File toBeRenamed = folder.newFolder(oldName);
        attemptRename(oldName, newName);
    }

    private void attemptRenameFile(String oldName, String newName) throws IOException {
        File toBeRenamed = folder.newFile(oldName);
        attemptRename(oldName, newName);
    }

    private void attemptRename(String oldName, String newName) throws IOException {
        assertEquals(1, folder.getRoot().list().length);

        store.move(oldName, newName);

        assertEquals(1, folder.getRoot().list().length);
    }

    @Test
    public void testGetResourceNotificationDispatcher_AtomicLazyInitialization() {
        final int nThreads = 64;
        final int nTasks = 4 * nThreads;
        final ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        try {
            Collection<Callable<FileSystemWatcher>> tasks =
                    IntStream.range(0, nTasks)
                            .mapToObj(
                                    i ->
                                            (Callable<FileSystemWatcher>)
                                                    () ->
                                                            ((FileSystemWatcher)
                                                                    store
                                                                            .getResourceNotificationDispatcher()))
                            .collect(Collectors.toList());

            List<FileSystemWatcher> watchers =
                    executorService
                            .invokeAll(tasks)
                            .stream()
                            .map(
                                    completedFuture -> {
                                        try {
                                            return completedFuture.get();
                                        } catch (InterruptedException | ExecutionException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                            .collect(Collectors.toList());

            assertEquals(nTasks, watchers.size());

            assertEquals(
                    "FileSystemWatcher initialization wasn't lazy and atomic",
                    1,
                    new HashSet<>(watchers).size());

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdownNow();
        }
    }
}
