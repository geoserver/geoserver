/* Copyright (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * Copyright (c) 2015 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotification.Kind;

/**
 * Simple ResourceWatcher implementation, only distributes events to this GeoServer instance.
 *
 * @author Niels Charlier
 */
public class SimpleResourceNotificationDispatcher implements ResourceNotificationDispatcher {

    private Map<String, List<ResourceListener>> handlers =
            new HashMap<String, List<ResourceListener>>();

    @Override
    public synchronized void addListener(String resource, ResourceListener listener) {
        List<ResourceListener> listeners = handlers.get(resource);
        if (listeners == null) {
            listeners = new ArrayList<ResourceListener>();
            handlers.put(resource, listeners);
        }
        listeners.add(listener);
    }

    @Override
    public synchronized boolean removeListener(String resource, ResourceListener listener) {
        List<ResourceListener> listeners = handlers.get(resource);
        if (listeners != null) {
            return listeners.remove(listener);
        }
        return false;
    }

    /** Send notification (without propagation), children may override this. */
    protected void changedInternal(ResourceNotification notification) {
        List<ResourceListener> listeners = handlers.get(notification.getPath());
        if (listeners != null) {
            // copy listeners into a new collection,
            // so that listeners can remove/add themselves if necessary
            listeners = new ArrayList<>(listeners);
            for (ResourceListener listener : listeners) {
                listener.changed(notification);
            }
        }
    }

    @Override
    public void changed(ResourceNotification notification) {

        changedInternal(notification);

        // if delete, propagate delete notifications to children, which can be found in the events
        // (see {@link createEvents})
        if (notification.getKind() == Kind.ENTRY_DELETE) {
            for (Event event : notification.events()) {
                if (!notification.getPath().equals(event.getPath())) {
                    changedInternal(
                            new ResourceNotification(
                                    event.getPath(),
                                    Kind.ENTRY_DELETE,
                                    notification.getTimestamp(),
                                    notification.events()));
                }
            }
        }

        // if create, propagate CREATE events to its created parents, which can be found in the
        // events (see {@link createEvents})
        Set<String> createdParents = new HashSet<String>();
        if (notification.getKind() == Kind.ENTRY_CREATE) {
            for (Event event : notification.events()) {
                if (!notification.getPath().equals(event.getPath())) {
                    createdParents.add(event.getPath());
                }
            }
        }

        // propagate any event to its direct parent (as MODIFY if not a created parent)
        String path = Paths.parent(notification.getPath());
        while (path != null) {
            boolean isCreate = createdParents.contains(path);
            changedInternal(
                    new ResourceNotification(
                            path,
                            isCreate ? Kind.ENTRY_CREATE : Kind.ENTRY_MODIFY,
                            notification.getTimestamp(),
                            relative(notification.events(), path)));

            // stop propagating after first modify
            path = isCreate ? Paths.parent(path) : null;
        }
    }

    private List<Event> relative(List<Event> events, String path) {
        // FileSystemResourceStore sends events with relative paths,
        // so we must do as well
        List<Event> result = new ArrayList<Event>();
        for (Event event : events) {
            result.add(new Event(event.getPath().replace(path + "/", ""), event.getKind()));
        }
        return result;
    }

    /**
     * Helper method to create all events for any operation (except rename) that happens to a
     * resource. This method should be called just before the action takes place in order to analyze
     * the effects properly.
     *
     * <p>Operations are assumed to be atomic except in the following two cases: (1) deleting causes
     * children to delete as well (2) creating causes non existing parents on the path to be created
     * as well
     *
     * <p>(Note: do not confuse the creation of notification events with propagation of
     * notifications).
     *
     * @param resource Resource being changed
     * @param kind Nature of resource change
     * @return List of notification events covering change
     */
    public static List<Event> createEvents(Resource resource, Kind kind) {
        List<Event> events = new ArrayList<Event>();

        events.add(new ResourceNotification.Event(resource.path(), kind));

        // (1)
        if (resource.getType() == Type.DIRECTORY && kind == Kind.ENTRY_DELETE) {
            for (Resource child : Resources.listRecursively(resource)) {
                events.add(new ResourceNotification.Event(child.path(), kind));
            }
        }

        // (2)
        if (kind == Kind.ENTRY_CREATE) {
            Resource parent = resource.parent();
            while (parent != null && !Resources.exists(parent)) {
                events.add(new ResourceNotification.Event(parent.path(), kind));
                parent = parent.parent();
            }
        }

        return events;
    }

    /**
     * Helper method to create all create/modify events caused by a rename/move operation. (delete
     * events must be created separately.) This method should be called just before the action takes
     * place in order to analyze the effects properly.
     *
     * <p>Rename/move causes children to be moved as well.
     *
     * @param src Resource being renamed
     * @param dest Target resource after renatmed
     * @return List of notification events covering all the modified Resources
     */
    public static List<Event> createRenameEvents(Resource src, Resource dest) {
        List<Event> events = new ArrayList<Event>();

        events.add(
                new ResourceNotification.Event(
                        dest.path(),
                        Resources.exists(dest) ? Kind.ENTRY_MODIFY : Kind.ENTRY_CREATE));

        for (Resource child : Resources.listRecursively(src)) {
            Resource newChild = dest.get(child.path().substring(src.path().length() + 1));
            events.add(
                    new ResourceNotification.Event(
                            newChild.path(),
                            Resources.exists(newChild) ? Kind.ENTRY_MODIFY : Kind.ENTRY_CREATE));
        }

        return events;
    }
}
