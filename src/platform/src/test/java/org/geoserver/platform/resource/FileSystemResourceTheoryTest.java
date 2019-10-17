/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class FileSystemResourceTheoryTest extends ResourceTheoryTest {

    FileSystemResourceStore store;

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Rule public ExpectedException expectedException = ExpectedException.none();

    @DataPoints
    public static String[] testPaths() {
        return new String[] {
            "FileA",
            "FileB",
            "DirC",
            "DirC/FileD",
            "DirE",
            "UndefF",
            "DirC/UndefF",
            "DirE/UndefF",
            "DirE/UndefG/UndefH/UndefI"
        };
    }

    @Override
    protected Resource getResource(String path) throws Exception {
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
    public void invalid() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Contains invalid .. path");
        store.get("..");
    }

    @Test
    public void fileEvents() throws Exception {
        File fileD = Paths.toFile(store.baseDirectory, "DirC/FileD");

        AwaitResourceListener listener = new AwaitResourceListener();

        store.get("DirC/FileD").addListener(listener);
        store.watcher.schedule(30, TimeUnit.MILLISECONDS);

        long before = fileD.lastModified();
        long after = touch(fileD);
        assertTrue("touched", after > before);

        ResourceNotification n = listener.await(1, TimeUnit.SECONDS);
        assertNotNull("detected event", n);

        assertEquals("file modified", Kind.ENTRY_MODIFY, n.getKind());
        assertTrue("Resource only", n.events().isEmpty());

        listener.reset();
        fileD.delete();
        n = listener.await(5, TimeUnit.SECONDS);
        assertEquals("file removed", Kind.ENTRY_DELETE, n.getKind());

        listener.reset();
        fileD.createNewFile();
        n = listener.await(5, TimeUnit.SECONDS);
        assertEquals("file created", Kind.ENTRY_CREATE, n.getKind());
        store.get("DirC/FileD").removeListener(listener);
    }

    /**
     * Changes the {@link File#lastModified() file.lastModified()} timestamp, making sure the result
     * differs from its current timestamp; may delay long enough to match file system resolution (2
     * seconds).
     *
     * <p>Example: Linux systems expect around 1 second resolution for file modification.
     *
     * @param file
     * @return resulting value of lastmodified
     */
    private long touch(File file) {
        long origional = file.lastModified();
        if (origional == 0l) {
            return 0l; // cannot modify a file that does not exsist
        }
        long after = origional;
        do {
            file.setLastModified(System.currentTimeMillis());
        } while (origional == (after = file.lastModified()));
        return after;
    }

    @Test
    public void eventNotification() throws InterruptedException {
        AwaitResourceListener listener = new AwaitResourceListener();

        ResourceNotification notification =
                new ResourceNotification(".", Kind.ENTRY_CREATE, 1_000_000L);
        CompletableFuture.runAsync(
                () -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    listener.changed(notification);
                });

        ResourceNotification n = listener.await(500, TimeUnit.MILLISECONDS);
        assertSame(notification, n);

        listener.reset();
        expectedException.expect(ConditionTimeoutException.class);
        listener.await(100, TimeUnit.MILLISECONDS); // expect timeout as no events will be sent!
    }

    @Test
    public void directoryEvents() throws Exception {
        File fileA = Paths.toFile(store.baseDirectory, "FileA");
        File fileB = Paths.toFile(store.baseDirectory, "FileB");
        File dirC = Paths.toFile(store.baseDirectory, "DirC");
        File fileD = Paths.toFile(store.baseDirectory, "DirC/FileD");
        File dirE = Paths.toFile(store.baseDirectory, "DirE");

        AwaitResourceListener listener = new AwaitResourceListener();
        store.get(Paths.BASE).addListener(listener);
        store.watcher.schedule(30, TimeUnit.MILLISECONDS);

        long before = fileB.lastModified();
        long after = touch(fileB);
        assertTrue("touched", after > before);
        ResourceNotification n = listener.await(500, TimeUnit.MILLISECONDS);

        assertEquals(Kind.ENTRY_MODIFY, n.getKind());
        assertEquals(Paths.BASE, n.getPath());
        assertEquals(1, n.events().size());
        Event e = n.events().get(0);
        assertEquals(Kind.ENTRY_MODIFY, e.getKind());
        assertEquals("FileB", e.getPath());

        listener.reset();
        fileA.delete();
        n = listener.await(5, TimeUnit.SECONDS);
        assertEquals(Kind.ENTRY_MODIFY, n.getKind());
        assertEquals(Paths.BASE, n.getPath());
        e = n.events().get(0);
        assertEquals(Kind.ENTRY_DELETE, e.getKind());
        assertEquals("FileA", e.getPath());

        listener.reset();
        fileA.createNewFile();
        n = listener.await(5, TimeUnit.SECONDS);
        assertEquals(Kind.ENTRY_MODIFY, n.getKind());
        assertEquals(Paths.BASE, n.getPath());
        e = n.events().get(0);
        assertEquals(Kind.ENTRY_CREATE, e.getKind());
        assertEquals("FileA", e.getPath());

        store.get(Paths.BASE).removeListener(listener);
    }

    /** ResourceListener that traps the next ResourceNotification for testing */
    static class AwaitResourceListener implements ResourceListener {
        private final AtomicReference<ResourceNotification> reference = new AtomicReference<>();

        @Override
        public void changed(ResourceNotification notify) {
            reference.set(notify);
        }

        public void reset() {
            this.reference.set(null);
        }

        /**
         * Awaits for {@link #changed} to be called for at most the given timeout before throwing a
         * {@link ConditionTimeoutException}, returning the event in case it was received.
         */
        public ResourceNotification await(int timeout, TimeUnit unit) {
            return Awaitility.await()
                    .pollDelay(50, TimeUnit.MILLISECONDS)
                    .atMost(timeout, unit)
                    .untilAtomic(this.reference, IsNull.notNullValue());
        }
    }

    @Override
    protected Resource getDirectory() {
        try {
            folder.newFolder("NonTestDir");
        } catch (IOException e) {
            fail();
        }
        return store.get("NonTestDir");
    }

    @Override
    protected Resource getResource() {
        try {
            folder.newFile("NonTestFile");
        } catch (IOException e) {
            fail();
        }
        return store.get("NonTestFile");
    }

    @Override
    protected Resource getUndefined() {
        return store.get("NonTestUndef");
    }
}
