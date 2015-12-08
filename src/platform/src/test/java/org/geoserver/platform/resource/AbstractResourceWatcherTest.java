/* Copyright (c) 2015 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * 
 * @author Niels Charlier
 *
 */
public abstract class AbstractResourceWatcherTest {
        
    protected FileSystemResourceStore store;
    protected ResourceWatcher watcher;
    
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
        TemporaryFolder folder = new TemporaryFolder();

        folder.create();
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
    
    protected abstract ResourceWatcher initWatcher() throws Exception;
    
    @Test
    public void testDeleteNotification() {
        Resource res = store.get("DirA");
        
        final CheckingResourceListener chkDirA = new CheckingResourceListener(Kind.ENTRY_DELETE), 
                chkDirC = new CheckingResourceListener(Kind.ENTRY_DELETE), 
                chkFileA1 = new CheckingResourceListener(Kind.ENTRY_DELETE), 
                chkFileA2 = new CheckingResourceListener(Kind.ENTRY_DELETE), 
                chkFileC1 = new CheckingResourceListener(Kind.ENTRY_DELETE), 
                chkFileC2 = new CheckingResourceListener(Kind.ENTRY_DELETE);
        
        watcher.addListener(res, chkDirA);
        watcher.addListener(res.get("FileA1"), chkFileA1);
        watcher.addListener(res.get("FileA2"), chkFileA2);
        watcher.addListener(res.get("DirC"), chkDirC);
        watcher.addListener(res.get("DirC/FileC1"), chkFileC1);
        watcher.addListener(res.get("DirC/FileC2"), chkFileC2);
        
        List<Event> events = SimpleResourceWatcher.createEvents(res, Kind.ENTRY_DELETE);        
        watcher.changed(new ResourceNotification("DirA", Kind.ENTRY_DELETE, System.currentTimeMillis(), events));
        
        //test that listeners received events
        assertTrue(chkDirA.isChecked());
        assertTrue(chkFileA1.isChecked());
        assertTrue(chkFileA2.isChecked());
        assertTrue(chkDirC.isChecked());
        assertTrue(chkFileC1.isChecked());
        assertTrue(chkFileC2.isChecked());
        
        //remove listeners
        assertTrue(watcher.removeListener(res, chkDirA));
        assertTrue(watcher.removeListener(res.get("FileA1"), chkFileA1));
        assertTrue(watcher.removeListener(res.get("FileA2"), chkFileA2));
        assertTrue(watcher.removeListener(res.get("DirC"), chkDirC));
        assertTrue(watcher.removeListener(res.get("DirC/FileC1"), chkFileC1));
        assertTrue(watcher.removeListener(res.get("DirC/FileC2"), chkFileC2));
        
    }
    
    @Test
    public void testModifyNotification() {
        Resource res = store.get("DirA/DirC/FileC1");
        
        final CheckingResourceListener chkDirA = new CheckingResourceListener(Kind.ENTRY_MODIFY), 
                chkDirC = new CheckingResourceListener(Kind.ENTRY_MODIFY), 
                chkFileC1 = new CheckingResourceListener(Kind.ENTRY_MODIFY);
        
        watcher.addListener(res, chkFileC1);
        watcher.addListener(store.get("DirA/DirC"), chkDirC);
        watcher.addListener(store.get("DirA"), chkDirA);
        
        List<Event> events = SimpleResourceWatcher.createEvents(res, Kind.ENTRY_MODIFY);        
        watcher.changed(new ResourceNotification("DirA/DirC/FileC1", Kind.ENTRY_MODIFY, System.currentTimeMillis(), events));
        
        //test that listeners received events
        assertTrue(chkDirA.isChecked());
        assertTrue(chkDirC.isChecked());
        assertTrue(chkFileC1.isChecked());
        
        //remove listeners
        assertTrue(watcher.removeListener(res, chkFileC1));
        assertTrue(watcher.removeListener(store.get("DirA/DirC"), chkDirC));
        assertTrue(watcher.removeListener(store.get("DirA"), chkDirA));
        
    }
    
    @Test
    public void testCreateNotification() {
        Resource res = store.get("DirA/DirC/DirD/FileQ");
        
        final CheckingResourceListener chkDirA = new CheckingResourceListener(Kind.ENTRY_MODIFY), 
                chkDirC = new CheckingResourceListener(Kind.ENTRY_MODIFY), 
                chkDirD = new CheckingResourceListener(Kind.ENTRY_CREATE),
                chkFileQ = new CheckingResourceListener(Kind.ENTRY_CREATE);
        
        watcher.addListener(res, chkFileQ);
        watcher.addListener(store.get("DirA/DirC/DirD"), chkDirD);
        watcher.addListener(store.get("DirA/DirC"), chkDirC);
        watcher.addListener(store.get("DirA"), chkDirA);
        
        List<Event> events = SimpleResourceWatcher.createEvents(res, Kind.ENTRY_CREATE);        
        watcher.changed(new ResourceNotification("DirA/DirC/DirD/FileQ", Kind.ENTRY_CREATE, System.currentTimeMillis(), events));
        
        //test that listeners received events
        assertTrue(chkDirA.isChecked());
        assertTrue(chkDirC.isChecked());
        assertTrue(chkDirD.isChecked());        
        assertTrue(chkFileQ.isChecked());
        
        //remove listeners
        assertTrue(watcher.removeListener(res, chkFileQ));
        assertTrue(watcher.removeListener(store.get("DirA/DirC/DirD"), chkDirD));
        assertTrue(watcher.removeListener(store.get("DirA/DirC"), chkDirC));
        assertTrue(watcher.removeListener(store.get("DirA"), chkDirA));
        
    }

}
