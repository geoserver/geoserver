/* Copyright (c) 2015 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotification.Kind;
import org.junit.Test;

/** @author Niels Charlier */
public class SimpleResourceNotificationDispatcherTest
        extends AbstractResourceNotificationDispatcherTest {

    @Override
    protected ResourceNotificationDispatcher initWatcher() {
        return new SimpleResourceNotificationDispatcher();
    }

    @Test
    public void testRenameEvents() {
        Resource src = store.get("DirA");
        Resource dest = store.get("DirB");

        List<Event> events = SimpleResourceNotificationDispatcher.createRenameEvents(src, dest);

        assertEquals(6, events.size());

        Set<String> set = new HashSet<String>();
        set.add("DirB");
        set.add("DirB/FileA1");
        set.add("DirB/FileA2");
        set.add("DirB/DirC");
        set.add("DirB/DirC/FileC1");
        set.add("DirB/DirC/FileC2");

        for (Event event : events) {
            String path = event.getPath();
            assertEquals(
                    path.equals("DirB") || path.equals("DirB/FileA2")
                            ? Kind.ENTRY_MODIFY
                            : Kind.ENTRY_CREATE,
                    event.getKind());
            assertTrue(set.remove(path));
        }

        assertTrue(set.isEmpty());
    }

    @Test
    public void testDeleteEvents() {
        Resource res = store.get("DirA");

        List<Event> events =
                SimpleResourceNotificationDispatcher.createEvents(res, Kind.ENTRY_DELETE);

        assertEquals(6, events.size());

        Set<String> set = new HashSet<String>();
        set.add("DirA");
        set.add("DirA/FileA1");
        set.add("DirA/FileA2");
        set.add("DirA/DirC");
        set.add("DirA/DirC/FileC1");
        set.add("DirA/DirC/FileC2");

        for (Event event : events) {
            assertEquals(Kind.ENTRY_DELETE, event.getKind());
            assertTrue(set.remove(event.getPath()));
        }

        assertTrue(set.isEmpty());
    }

    @Test
    public void testCreateEvents() {
        Resource res = store.get("DirD/DirE/DirF/FileQ");

        List<Event> events =
                SimpleResourceNotificationDispatcher.createEvents(res, Kind.ENTRY_CREATE);

        assertEquals(4, events.size());

        Set<String> set = new HashSet<String>();
        set.add("DirD");
        set.add("DirD/DirE");
        set.add("DirD/DirE/DirF");
        set.add("DirD/DirE/DirF/FileQ");

        for (Event event : events) {
            assertEquals(Kind.ENTRY_CREATE, event.getKind());
            assertTrue(set.remove(event.getPath()));
        }

        assertTrue(set.isEmpty());
    }

    @Test
    public void testPropagation() throws IOException {
        SimpleResourceNotificationDispatcher dispatch = new SimpleResourceNotificationDispatcher();

        AtomicReference<ResourceNotification> dirEvent = new AtomicReference<>();

        dispatch.addListener(
                "DirB",
                new ResourceListener() {

                    @Override
                    public void changed(ResourceNotification notify) {
                        dirEvent.set(notify);
                    }
                });

        dispatch.changed(
                new ResourceNotification(
                        "DirB/DirNew/FileNew",
                        Kind.ENTRY_CREATE,
                        System.currentTimeMillis(),
                        SimpleResourceNotificationDispatcher.createEvents(
                                store.get("DirB/DirNew/FileNew"), Kind.ENTRY_CREATE)));

        assertNotNull(dirEvent.get());
        assertEquals(Kind.ENTRY_MODIFY, dirEvent.get().getKind());
        assertEquals("DirB", dirEvent.get().getPath());
        assertEquals(2, dirEvent.get().events().size());
        assertEquals(Kind.ENTRY_CREATE, dirEvent.get().events().get(0).getKind());
        assertEquals("DirNew/FileNew", dirEvent.get().events().get(0).getPath());
        assertEquals(Kind.ENTRY_CREATE, dirEvent.get().events().get(1).getKind());
        assertEquals("DirNew", dirEvent.get().events().get(1).getPath());
    }
}
