/* Copyright (c) 2015 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** @author Niels Charlier */
public abstract class AbstractResourceNotificationDispatcherTest {

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    protected FileSystemResourceStore store;
    protected ResourceNotificationDispatcher watcher;

    protected static class CheckingResourceListener implements ResourceListener {
        private boolean checked = false;
        private Kind kind;

        public CheckingResourceListener(Kind kind) {
            this.kind = kind;
        }

        @Override
        public void changed(ResourceNotification notify) {
            if (kind == notify.getKind()) {
                checked = true;
            }
        }

        public boolean isChecked() {
            return checked;
        }
    }

    @Before
    public void setup() throws Exception {
        File dirA = folder.newFolder("DirA");
        File dirB = folder.newFolder("DirB");
        File dirC = new File(dirA, "DirC");
        dirC.mkdir();
        (new File(dirA, "FileA1")).createNewFile();
        (new File(dirA, "FileA2")).createNewFile();
        (new File(dirB, "FileA2")).createNewFile();
        (new File(dirB, "FileB1")).createNewFile();
        (new File(dirB, "FileB2")).createNewFile();
        (new File(dirC, "FileC1")).createNewFile();
        (new File(dirC, "FileC2")).createNewFile();

        store = new FileSystemResourceStore(folder.getRoot());
        watcher = initWatcher();
    }

    protected abstract ResourceNotificationDispatcher initWatcher() throws Exception;

    @Test
    public void testDeleteNotification() {
        Resource res = store.get("DirA");

        final CheckingResourceListener chkDirA = new CheckingResourceListener(Kind.ENTRY_DELETE),
                chkDirC = new CheckingResourceListener(Kind.ENTRY_DELETE),
                chkFileA1 = new CheckingResourceListener(Kind.ENTRY_DELETE),
                chkFileA2 = new CheckingResourceListener(Kind.ENTRY_DELETE),
                chkFileC1 = new CheckingResourceListener(Kind.ENTRY_DELETE),
                chkFileC2 = new CheckingResourceListener(Kind.ENTRY_DELETE);

        watcher.addListener(res.path(), chkDirA);
        watcher.addListener(res.get("FileA1").path(), chkFileA1);
        watcher.addListener(res.get("FileA2").path(), chkFileA2);
        watcher.addListener(res.get("DirC").path(), chkDirC);
        watcher.addListener(res.get("DirC/FileC1").path(), chkFileC1);
        watcher.addListener(res.get("DirC/FileC2").path(), chkFileC2);

        List<Event> events =
                SimpleResourceNotificationDispatcher.createEvents(res, Kind.ENTRY_DELETE);
        watcher.changed(
                new ResourceNotification(
                        "DirA", Kind.ENTRY_DELETE, System.currentTimeMillis(), events));

        // test that listeners received events
        assertTrue(chkDirA.isChecked());
        assertTrue(chkFileA1.isChecked());
        assertTrue(chkFileA2.isChecked());
        assertTrue(chkDirC.isChecked());
        assertTrue(chkFileC1.isChecked());
        assertTrue(chkFileC2.isChecked());

        // remove listeners
        assertTrue(watcher.removeListener(res.path(), chkDirA));
        assertTrue(watcher.removeListener(res.get("FileA1").path(), chkFileA1));
        assertTrue(watcher.removeListener(res.get("FileA2").path(), chkFileA2));
        assertTrue(watcher.removeListener(res.get("DirC").path(), chkDirC));
        assertTrue(watcher.removeListener(res.get("DirC/FileC1").path(), chkFileC1));
        assertTrue(watcher.removeListener(res.get("DirC/FileC2").path(), chkFileC2));
    }

    @Test
    public void testDeleteWhileListening() {
        Resource res = store.get("DirA");

        final ResourceListener deletingListener =
                new ResourceListener() {
                    @Override
                    public void changed(ResourceNotification notify) {
                        assertTrue(watcher.removeListener(notify.getPath(), this));
                    }
                };

        watcher.addListener(res.path(), deletingListener);

        watcher.changed(
                new ResourceNotification(
                        "DirA",
                        Kind.ENTRY_DELETE,
                        System.currentTimeMillis(),
                        Collections.emptyList()));

        // verify already deleted
        assertFalse(watcher.removeListener(res.path(), deletingListener));
    }

    @Test
    public void testModifyNotification() {
        Resource res = store.get("DirA/DirC/FileC1");

        final CheckingResourceListener chkDirA = new CheckingResourceListener(Kind.ENTRY_MODIFY),
                chkDirC = new CheckingResourceListener(Kind.ENTRY_MODIFY),
                chkFileC1 = new CheckingResourceListener(Kind.ENTRY_MODIFY);

        watcher.addListener(res.path(), chkFileC1);
        watcher.addListener(store.get("DirA/DirC").path(), chkDirC);
        watcher.addListener(store.get("DirA").path(), chkDirA);

        List<Event> events =
                SimpleResourceNotificationDispatcher.createEvents(res, Kind.ENTRY_MODIFY);
        watcher.changed(
                new ResourceNotification(
                        "DirA/DirC/FileC1", Kind.ENTRY_MODIFY, System.currentTimeMillis(), events));

        // test that listeners received events
        assertFalse(chkDirA.isChecked());
        assertTrue(chkDirC.isChecked());
        assertTrue(chkFileC1.isChecked());

        // remove listeners
        assertTrue(watcher.removeListener(res.path(), chkFileC1));
        assertTrue(watcher.removeListener(store.get("DirA/DirC").path(), chkDirC));
        assertTrue(watcher.removeListener(store.get("DirA").path(), chkDirA));
    }

    @Test
    public void testCreateNotification() {
        Resource res = store.get("DirA/DirC/DirD/FileQ");

        final CheckingResourceListener chkDirA = new CheckingResourceListener(Kind.ENTRY_MODIFY),
                chkDirC = new CheckingResourceListener(Kind.ENTRY_MODIFY),
                chkDirD = new CheckingResourceListener(Kind.ENTRY_CREATE),
                chkFileQ = new CheckingResourceListener(Kind.ENTRY_CREATE);

        watcher.addListener(res.path(), chkFileQ);
        watcher.addListener(store.get("DirA/DirC/DirD").path(), chkDirD);
        watcher.addListener(store.get("DirA/DirC").path(), chkDirC);
        watcher.addListener(store.get("DirA").path(), chkDirA);

        List<Event> events =
                SimpleResourceNotificationDispatcher.createEvents(res, Kind.ENTRY_CREATE);
        watcher.changed(
                new ResourceNotification(
                        "DirA/DirC/DirD/FileQ",
                        Kind.ENTRY_CREATE,
                        System.currentTimeMillis(),
                        events));

        // test that listeners received events
        assertFalse(chkDirA.isChecked());
        assertTrue(chkDirC.isChecked());
        assertTrue(chkDirD.isChecked());
        assertTrue(chkFileQ.isChecked());

        // remove listeners
        assertTrue(watcher.removeListener(res.path(), chkFileQ));
        assertTrue(watcher.removeListener(store.get("DirA/DirC/DirD").path(), chkDirD));
        assertTrue(watcher.removeListener(store.get("DirA/DirC").path(), chkDirC));
        assertTrue(watcher.removeListener(store.get("DirA").path(), chkDirA));
    }
}
