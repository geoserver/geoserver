/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

/**
 * Empty ResourceStore implementation (any attempt to access content will result in
 * IllegalStateException). This implementation prevents client code from requiring null checks on
 * {@link ResourceStore#get(String)}. IllegalStateException are thrown by in(), out() and file()
 * which are the usual methods clients require error handling.
 */
final class NullResourceStore implements ResourceStore {
    final long MODIFIED = System.currentTimeMillis();
    final LockProvider locks = new NullLockProvider();

    @Override
    public Resource get(final String resourcePath) {
        return new Resource() {

            String path = resourcePath;

            @Override
            public String path() {
                return path;
            }

            @Override
            public String name() {
                return Paths.name(path);
            }

            @Override
            public InputStream in() {
                throw new IllegalStateException("Unable to read from ResourceStore.EMPTY");
            }

            @Override
            public Lock lock() {
                return locks.acquire(path);
            }

            @Override
            public void addListener(ResourceListener listener) {
                // no events provided
            }

            @Override
            public void removeListener(ResourceListener listener) {
                // no events provided
            }

            @Override
            public OutputStream out() {
                throw new IllegalStateException("Unable to write to ResourceStore.EMPTY");
            }

            @Override
            public File file() {
                throw new IllegalStateException("No file access to ResourceStore.EMPTY");
            }

            @Override
            public File dir() {
                throw new IllegalStateException("No directory access to ResourceStore.EMPTY");
            }

            @Override
            public long lastmodified() {
                return MODIFIED;
            }

            @Override
            public Resource parent() {
                return ResourceStore.EMPTY.get(Paths.parent(path));
            }

            public Resource get(String resourcePath) {
                return ResourceStore.EMPTY.get(Paths.path(this.path, resourcePath));
            }

            @Override
            public List<Resource> list() {
                return Collections.emptyList();
            }

            @Override
            public Type getType() {
                return Type.UNDEFINED;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((path == null) ? 0 : path.hashCode());
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (obj == null) return false;
                if (getClass() != obj.getClass()) return false;
                Resource other = (Resource) obj;
                if (path == null) {
                    if (other.path() != null) return false;
                } else if (!path.equals(other.path())) return false;
                return true;
            }

            @Override
            public String toString() {
                return path;
            }

            @Override
            public boolean delete() {
                return false;
            }

            @Override
            public boolean renameTo(Resource dest) {
                return false;
            }
        };
    }

    public String toString() {
        return "NullResourceStore";
    }

    @Override
    public boolean remove(String path) {
        return false; // unable to remove empty resource
    }

    @Override
    public boolean move(String path, String target) {
        return false; // unable to move empty resource
    }

    @Override
    public ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        return null;
    }
}
