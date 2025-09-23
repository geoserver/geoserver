/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

public class FileSystemResourceTheoryTest extends ResourceTheoryTest {

    /**
     * On a local machine this would be long wait, but Github action VMs are slow and erratic, let's give them more time
     */
    private static final int MAX_WAIT_SEC = 60;

    FileSystemResourceStore store;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public TestName testName = new TestName();

    @DataPoints
    public static String[] getTestPaths() {
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

    @After
    public void after() throws Exception {
        if (store != null && store.watcher.get() != null) {
            store.watcher.get().destroy();
        }
    }

    @Test
    public void invalid() {

        assertThrows(IllegalArgumentException.class, () -> store.get(".."));
    }

    @Test
    public void fileEvents() throws Exception {
        File fileD = new File(store.baseDirectory, "DirC/FileD");

        AwaitResourceListener listener = new AwaitResourceListener();

        store.get("DirC/FileD").addListener(listener);
        ((FileSystemWatcher) store.getResourceNotificationDispatcher()).schedule(30, MILLISECONDS);

        long before = fileD.lastModified();
        long after = touch(fileD);
        assertTrue("touched", after > before);

        ResourceNotification n = listener.await(MAX_WAIT_SEC, SECONDS);
        assertNotNull("detected event", n);

        assertEquals("file modified", Kind.ENTRY_MODIFY, n.getKind());
        assertTrue("Resource only", n.events().isEmpty());

        listener.reset();
        fileD.delete();
        n = listener.await(MAX_WAIT_SEC, SECONDS);
        assertEquals("file removed", Kind.ENTRY_DELETE, n.getKind());

        listener.reset();
        fileD.createNewFile();
        n = listener.await(MAX_WAIT_SEC, SECONDS);
        assertEquals("file created", Kind.ENTRY_CREATE, n.getKind());
        store.get("DirC/FileD").removeListener(listener);
    }

    /**
     * Changes the {@link File#lastModified() file.lastModified()} timestamp, making sure the result differs from its
     * current timestamp; may delay long enough to match file system resolution.
     *
     * <p>Just like with the default behavior of the Unix touch command, if the file does not exist, creates a new,
     * empty file.
     *
     * <p>Example: Linux systems expect around 1 second resolution for file modification.
     *
     * @return resulting value of lastmodified
     */
    private long touch(File file) {
        long origional = file.lastModified();
        if (origional == 0l) {
            try {
                Files.createFile(file.toPath());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return file.lastModified();
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

        ResourceNotification notification = new ResourceNotification(".", Kind.ENTRY_CREATE, 1_000_000L);
        CompletableFuture.runAsync(() -> listener.changed(notification));

        ResourceNotification n = listener.await(MAX_WAIT_SEC, SECONDS);
        assertSame(notification, n);

        listener.reset();
        // expect timeout as no events will be sent!
        assertThrows(ConditionTimeoutException.class, () -> {
            listener.await(100, MILLISECONDS); // expect timeout as no events will be sent!
        });
    }

    @Test
    public void directoryEvents() throws Exception {
        File fileA = new File(store.baseDirectory, "FileA");
        File fileB = new File(store.baseDirectory, "FileB");

        AwaitResourceListener listener = new AwaitResourceListener();
        store.get(Paths.BASE).addListener(listener);
        ((FileSystemWatcher) store.getResourceNotificationDispatcher()).schedule(100, MILLISECONDS);

        long before = fileB.lastModified();
        long after = touch(fileB);
        assertTrue("touched", after > before);
        ResourceNotification n = listener.await(MAX_WAIT_SEC, SECONDS);

        assertEquals(Kind.ENTRY_MODIFY, n.getKind());
        assertEquals(Paths.BASE, n.getPath());
        assertEquals(1, n.events().size());
        Event e = n.events().get(0);
        assertEquals(Kind.ENTRY_MODIFY, e.getKind());
        assertEquals("FileB", e.getPath());

        listener.reset();
        fileA.delete();
        n = listener.await(MAX_WAIT_SEC, SECONDS);
        assertEquals(Kind.ENTRY_MODIFY, n.getKind());
        assertEquals(Paths.BASE, n.getPath());
        e = n.events().get(0);
        assertEquals(Kind.ENTRY_DELETE, e.getKind());
        assertEquals("FileA", e.getPath());

        listener.reset();
        fileA.createNewFile();
        n = listener.await(MAX_WAIT_SEC, SECONDS);
        assertEquals(Kind.ENTRY_MODIFY, n.getKind());
        assertEquals(Paths.BASE, n.getPath());
        e = n.events().get(0);
        assertEquals(Kind.ENTRY_CREATE, e.getKind());
        assertEquals("FileA", e.getPath());

        store.get(Paths.BASE).removeListener(listener);
    }

    @Test
    public void emptyDirectoryCreateEventShouldNotBeRaised() throws Exception {
        final String dirName = testName.getMethodName();
        File watchedDir = new File(store.baseDirectory, dirName);

        FileSystemWatcher watcher = (FileSystemWatcher) store.getResourceNotificationDispatcher();
        // set a shorter poll delay
        watcher.schedule(100, MILLISECONDS);

        AwaitResourceListener listener = new AwaitResourceListener();

        // listen to changes on a resource that doesn't yet exist
        watcher.addListener(watchedDir.getName(), listener);
        assertFalse(watchedDir.exists());
        assertTrue(watchedDir.mkdir());

        // empty directory create events are not raised since we're watching for directory contents
        assertThrows(ConditionTimeoutException.class, () -> listener.await(100, MILLISECONDS));
    }

    @Test
    public void directoryCreateEventWithContents() throws Exception {
        final String dirName = testName.getMethodName();
        File watchedDir = new File(store.baseDirectory, dirName);
        File fileA = new File(watchedDir, "FileA");

        FileSystemWatcher watcher = (FileSystemWatcher) store.getResourceNotificationDispatcher();
        // set a shorter poll delay
        watcher.schedule(100, MILLISECONDS);

        AwaitResourceListener listener = new AwaitResourceListener();

        // listen to changes on a resource that doesn't yet exist
        watcher.addListener(watchedDir.getName(), listener);
        assertFalse(watchedDir.exists());
        assertTrue(watchedDir.mkdir());
        touch(fileA);
        assertTrue(fileA.exists());

        ResourceNotification n = listener.await(MAX_WAIT_SEC, SECONDS);
        assertEquals(dirName, n.getPath());
        assertEquals(Kind.ENTRY_CREATE, n.getKind());
        assertEquals(1, n.events().size());
        Event event = n.events().get(0);
        assertEquals(Kind.ENTRY_CREATE, event.getKind());
        assertEquals("FileA", event.getPath());
    }

    @Test
    public void dynamicAsyncDirectoryEvents() throws Exception {
        final String dirName = testName.getMethodName();
        final File watchedDir = new File(store.baseDirectory, dirName);

        FileSystemWatcher watcher = (FileSystemWatcher) store.getResourceNotificationDispatcher();
        // set a shorter poll delay
        watcher.schedule(100, MILLISECONDS);
        List<ResourceNotification> notifications = new CopyOnWriteArrayList<>();
        watcher.addListener(dirName, notifications::add);

        int fileCount = 256;
        final Set<String> files = IntStream.range(0, fileCount)
                .mapToObj(i -> "File%d".formatted(i))
                .collect(Collectors.toSet());
        // async create files with delay
        final BlockingQueue<String> names = new ArrayBlockingQueue<>(files.size(), true, files);
        final Callable<Void> asyncFileModifier = () -> {
            String resource;
            while ((resource = names.poll()) != null) {
                try {
                    Thread.sleep(10);
                    File f = new File(watchedDir, resource);
                    f.getParentFile().mkdir(); // create parent if doesn't yet exist
                    touch(f);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        };

        final int nthreads = 8;
        final ExecutorService executorService = Executors.newFixedThreadPool(nthreads);
        try { // returns once all tasks finished
            executorService.invokeAll(Collections.nCopies(nthreads, asyncFileModifier));
        } finally {
            executorService.shutdown();
        }
        // give file watcher a chance to catch up with latest events, allow time for slow platforms
        // while exiting soon for faster ones
        await().atMost(40, SECONDS)
                .pollDelay(20, MILLISECONDS)
                .until(() -> getEventCounts(notifications), CoreMatchers.equalTo(fileCount));

        assertEquals(
                1,
                notifications.stream()
                        .filter(n -> n.getKind() == Kind.ENTRY_CREATE)
                        .count());
        assertEquals(
                notifications.size() - 1,
                notifications.stream()
                        .filter(n -> n.getKind() == Kind.ENTRY_MODIFY)
                        .count());

        List<Event> fileEvents = notifications.stream()
                .map(ResourceNotification::events)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        assertEquals(files.size(), fileEvents.size());
    }

    private int getEventCounts(List<ResourceNotification> notifications) {
        return (int) notifications.stream()
                .map(ResourceNotification::events)
                .flatMap(List::stream)
                .count();
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
                    .pollInterval(5, MILLISECONDS)
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
