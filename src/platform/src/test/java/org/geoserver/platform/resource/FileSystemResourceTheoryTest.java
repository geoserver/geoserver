/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.rules.TemporaryFolder;

public class FileSystemResourceTheoryTest extends ResourceTheoryTest {

    FileSystemResourceStore store;

    @Rule public TemporaryFolder folder = new TemporaryFolder();

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
        try {
            Resource resource = store.get("..");
            assertNotNull(resource);
            fail(".. invalid");
        } catch (IllegalArgumentException expected) {
        }
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

        ResourceNotification n = listener.await(5, TimeUnit.SECONDS);
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
     * Must delay long enough to match file system resolution (2 seconds).
     *
     * <p>Example: Linux systems expect around 1 second resolution for file modification.
     *
     * @return resulting value of lastmodified
     */
    private long touch(File file) throws InterruptedException {
        long origional = file.lastModified();
        if (origional == 0l) {
            return 0l; // cannot modify a file that does not exsist
        }
        Thread.sleep(2000); // wait two seconds
        long modifided = System.currentTimeMillis();
        file.setLastModified(modifided);
        for (modifided = file.lastModified();
                (modifided - origional) < 2000;
                modifided = file.lastModified()) {
            file.setLastModified(System.currentTimeMillis());
            Thread.sleep(1000);
        }
        return modifided;
    }

    @Test
    public void eventNotification() throws InterruptedException {
        AwaitResourceListener listener = new AwaitResourceListener();

        ResourceNotification n =
                listener.await(5, TimeUnit.SECONDS); // expect timeout as no events will be sent!
        assertNull("No events expected", n);
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
        ResourceNotification n = listener.await(5, TimeUnit.SECONDS);

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
    static class AwaitResourceListener extends Await<ResourceNotification>
            implements ResourceListener {
        @Override
        public void changed(ResourceNotification notify) {
            notify(notify);
        }
    }
    /**
     * Support class to efficiently wait for event notification.
     *
     * @author Jody Garnett (Boundless)
     * @param <T> Event Type
     */
    abstract static class Await<T> {

        Lock lock = new ReentrantLock(true);

        Condition condition = lock.newCondition();

        private T event = null;

        public void notify(T notification) {
            // System.out.println("Arrived:"+notification);
            lock.lock();
            try {
                if (this.event == null) {
                    this.event = notification;
                }
                condition.signalAll(); // wake up your event is ready
            } finally {
                lock.unlock();
            }
        }

        public T await() throws InterruptedException {
            return await(5, TimeUnit.SECONDS);
        }
        /**
         * Wait for event notification.
         *
         * <p>If the event has arrived already this method will return immediately, if not we will
         * wait for signal. If the event still has not arrived after five seconds null will be
         * returned.
         *
         * @return Notification event, or null if it does not arrive within 5 seconds
         */
        public T await(long howlong, TimeUnit unit) throws InterruptedException {
            final long DELAY = unit.convert(howlong, TimeUnit.MILLISECONDS);
            lock.lock();
            try {
                if (this.event == null) {
                    long mark = System.currentTimeMillis();
                    while (this.event == null) {
                        long check = System.currentTimeMillis();
                        if (mark + DELAY < check) {
                            return null; // event did not show up!
                        }
                        boolean signal = condition.await(1, TimeUnit.SECONDS);
                        // System.out.println("check wait="+signal+" time="+check+"
                        // notify="+this.event);
                    }
                }
            } finally {
                lock.unlock();
            }
            return this.event;
        }

        public void reset() {
            lock.lock();
            try {
                this.event = null;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public String toString() {
            return "Await [event=" + event + "]";
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
