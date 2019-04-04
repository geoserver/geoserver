/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver;

import com.google.common.collect.Lists;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.platform.resource.LockProvider;
import org.geoserver.platform.resource.NullLockProvider;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.ResourceNotification.Event;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.SimpleResourceNotificationDispatcher;

public class HeapResourceStore implements ResourceStore {

    HeapResource root;

    HeapResourceNotificationDispatcher dispatcher;

    protected LockProvider lockProvider = new NullLockProvider();

    public HeapResourceStore() {
        root = new HeapResource(this);
        dispatcher = new HeapResourceNotificationDispatcher();
    }

    @Override
    public Resource get(String path) {
        return root.get(path);
    }

    @Override
    public boolean remove(String path) {
        HeapResource resource = (HeapResource) get(path);
        if (!resource.getType().equals(Type.UNDEFINED)) {
            return resource.delete();
        }

        return false;
    }

    @Override
    public boolean move(String path, String target) {
        HeapResource resource = (HeapResource) get(path);
        if (!resource.getType().equals(Type.UNDEFINED)) {
            return resource.renameTo(get(target));
        }
        return false;
    }

    @Override
    public ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        return dispatcher;
    }

    class HeapResourceNotificationDispatcher implements ResourceNotificationDispatcher {

        Map<String, List<ResourceListener>> listeners = new HashMap<>();

        @Override
        public void addListener(String resource, ResourceListener listener) {
            List<ResourceListener> resourceListeners = listeners.get(resource);
            if (resourceListeners == null) {
                resourceListeners = new ArrayList<>();
                listeners.put(resource, resourceListeners);
            }
            resourceListeners.add(listener);
        }

        @Override
        public boolean removeListener(String resource, ResourceListener listener) {
            List<ResourceListener> resourceListeners = listeners.get(resource);
            if (resourceListeners != null) {
                return resourceListeners.remove(listener);
            }
            return true;
        }

        @Override
        public void changed(ResourceNotification notification) {
            List<ResourceListener> originalListeners = listeners.get(notification.getPath());
            // Copy list, since some handlers try to remove themselves on notifications
            if (originalListeners != null) {
                List<ResourceListener> resourceListeners = new ArrayList<>(originalListeners);

                for (ResourceListener listener : resourceListeners) {
                    listener.changed(notification);
                }
            }

            // if delete, propagate delete notifications to children, which can be found in the
            // events (see {@link createEvents})
            if (notification.getKind() == ResourceNotification.Kind.ENTRY_DELETE) {
                for (ResourceNotification.Event event : notification.events()) {
                    if (!notification.getPath().equals(event.getPath())) {
                        this.changed(
                                new ResourceNotification(
                                        event.getPath(),
                                        ResourceNotification.Kind.ENTRY_DELETE,
                                        notification.getTimestamp(),
                                        Collections.emptyList()));
                    }
                }
            }

            // if create, propagate CREATE events to its created parents, which can be found in the
            // events (see {@link createEvents})
            Set<String> createdParents = new HashSet<>();
            if (notification.getKind() == ResourceNotification.Kind.ENTRY_CREATE) {
                for (ResourceNotification.Event event : notification.events()) {
                    if (!notification.getPath().equals(event.getPath())) {
                        createdParents.add(event.getPath());
                    }
                }
            }

            // propagate any event to its direct parent (as MODIFY if not a created parent)
            List<String> paths = Lists.newArrayList(Paths.names(notification.getPath()));
            paths.remove(paths.size() - 1);
            while (paths.size() > 0) {
                String path = Paths.path(paths.toArray(new String[0]));
                boolean isCreate = createdParents.contains(path);
                this.changed(
                        new ResourceNotification(
                                path,
                                isCreate
                                        ? ResourceNotification.Kind.ENTRY_CREATE
                                        : ResourceNotification.Kind.ENTRY_MODIFY,
                                notification.getTimestamp(),
                                notification.events()));

                // stop propagating after first modify
                paths.remove(paths.size() - 1);
            }
        }
    }

    class HeapResource implements Resource {

        private String name;

        private String path;

        private HeapResource parent;

        private List<HeapResource> children;

        private Type type;

        private byte[] bytes;

        private long lastModified;

        private final HeapResourceStore store;

        public HeapResource(HeapResourceStore store) {
            this(store, null, null);
        }

        public HeapResource(HeapResourceStore store, String name, HeapResource parent) {
            this.store = store;
            this.name = name;
            if (name == null) {
                this.type = Type.DIRECTORY;
                this.parent = null;
                this.path = null;
            } else {
                this.type = Type.UNDEFINED;
                this.parent = parent;
                this.path = buildPath();
            }
            this.lastModified = 0L; // no timestamp until it's first written
            this.bytes = null;
            this.children = new LinkedList<>();
        }

        private String buildPath() {
            List<String> pathNames = new LinkedList<>();
            pathNames.add(name);
            HeapResource currParent = parent;
            while (currParent.name != null) {
                pathNames.add(currParent.name);
                currParent = currParent.parent;
            }
            String[] paths = Lists.reverse(pathNames).toArray(new String[0]);
            return Paths.path(paths);
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Lock lock() {
            return lockProvider.acquire(toString());
        }

        @Override
        public void addListener(ResourceListener listener) {}

        @Override
        public void removeListener(ResourceListener listener) {}

        @Override
        public InputStream in() {
            if (getType().equals(Type.UNDEFINED) || getType().equals(Type.DIRECTORY)) {
                throw new UnsupportedOperationException();
            }
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public OutputStream out() {
            final ResourceNotification createNotification;
            if (bytes == null && getType().equals(Type.UNDEFINED)) {
                List<Event> events =
                        SimpleResourceNotificationDispatcher.createEvents(
                                this, ResourceNotification.Kind.ENTRY_CREATE);
                createNotification =
                        new ResourceNotification(
                                path,
                                ResourceNotification.Kind.ENTRY_CREATE,
                                System.currentTimeMillis(),
                                events);
                this.type = Type.RESOURCE;
            } else {
                createNotification = null;
            }
            if (getType().equals(Type.DIRECTORY)) {
                throw new UnsupportedOperationException();
            }
            return new OutputStream() {
                ByteArrayOutputStream delegate = new ByteArrayOutputStream();

                @Override
                public void close() throws IOException {
                    final Lock lock = lock();
                    try {
                        // close the stream first
                        delegate.close();
                        bytes = delegate.toByteArray();
                        lastModified = System.currentTimeMillis();
                        // fire any deferred create events stored
                        if (createNotification != null) {
                            dispatcher.changed(createNotification);
                        }
                        // now fire the modified events
                        List<Event> events =
                                SimpleResourceNotificationDispatcher.createEvents(
                                        HeapResource.this, ResourceNotification.Kind.ENTRY_MODIFY);

                        dispatcher.changed(
                                new ResourceNotification(
                                        path(),
                                        ResourceNotification.Kind.ENTRY_MODIFY,
                                        System.currentTimeMillis(),
                                        events));
                    } finally {
                        lock.release();
                    }
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    delegate.write(b, off, len);
                }

                @Override
                public void flush() throws IOException {
                    delegate.flush();
                }

                @Override
                public void write(byte[] b) throws IOException {
                    delegate.write(b);
                }

                @Override
                public void write(int b) throws IOException {
                    delegate.write(b);
                }
            };
        }

        @Override
        public File file() {
            throw new UnsupportedOperationException();
        }

        @Override
        public File dir() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long lastmodified() {
            return lastModified;
        }

        @Override
        public Resource parent() {
            return parent;
        }

        @Override
        public Resource get(String resourcePath) {
            String[] pathNames = resourcePath.split("/");
            int pathIndex = 0;
            HeapResource resource = this;
            while (pathIndex < pathNames.length) {
                if (resource.getType().equals(Type.UNDEFINED)) {
                    resource.type = Type.DIRECTORY;
                }
                boolean found = false;
                String pathName = pathNames[pathIndex];
                for (HeapResource child : resource.children) {
                    if (child.name.equals(pathName)) {
                        resource = child;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    HeapResource newResource = new HeapResource(store, pathName, resource);
                    resource.children.add(newResource);
                    resource = newResource;
                }
                pathIndex++;
            }
            return resource;
        }

        @Override
        public List<Resource> list() {
            List<Resource> resources = Lists.newArrayList(children);
            return resources;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public boolean delete() {
            boolean deleted = false;
            if (!getType().equals(Type.UNDEFINED)) {
                if (parent != null) {
                    parent.children.remove(this);
                }
                type = Type.UNDEFINED;
                children.clear();
                bytes = null;
                deleted = true;
                List<Event> events =
                        SimpleResourceNotificationDispatcher.createEvents(
                                this, ResourceNotification.Kind.ENTRY_DELETE);
                dispatcher.changed(
                        new ResourceNotification(
                                path,
                                ResourceNotification.Kind.ENTRY_DELETE,
                                System.currentTimeMillis(),
                                events));
            }
            return deleted;
        }

        @Override
        public boolean renameTo(Resource dest) {
            if (dest == this) {
                return false;
            }
            List<ResourceNotification.Event> eventsDelete =
                    SimpleResourceNotificationDispatcher.createEvents(
                            this, ResourceNotification.Kind.ENTRY_DELETE);
            List<ResourceNotification.Event> eventsRename =
                    SimpleResourceNotificationDispatcher.createRenameEvents(this, dest);
            this.path = dest.path();
            this.name = dest.name();
            this.parent = (HeapResource) dest.parent();
            this.parent.children.remove((HeapResource) dest);
            this.parent.children.add(this);
            dispatcher.changed(
                    new ResourceNotification(
                            path(),
                            ResourceNotification.Kind.ENTRY_DELETE,
                            System.currentTimeMillis(),
                            eventsDelete));
            dispatcher.changed(
                    new ResourceNotification(
                            path(),
                            eventsRename.get(0).getKind(),
                            System.currentTimeMillis(),
                            eventsRename));
            return true;
        }
    }
}
