/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Notification of resource changes.
 *
 * <p>Notification is provided as a delta modified resources:
 *
 * <ul>
 *   <li>Listeners on a single resource will be notified on resource change to that resource.
 *       <p>A listener to path="user_projections/epsg.properties" receive notification on change to
 *       the <b>epsg.properties</b> file. This notification will consist of of delta=<code>
 *       user_projections/epsg.properties</code>
 *   <li>Listeners on a directory will be notified on any resource change in the directory. The
 *       delta will include any modified directories. A listener on path="style" is notified on
 *       change to <b>style/pophatch.sld</b> and <b>style/icons/city.png</b>. The change to these
 *       two files is represented with delta consisting of delta=<code>
 *       style,style/icons,style/icons/city.png,style/pophatch.sld</code>
 *   <li>Removed resources may be represented in notification, but will have reverted to {@link
 *       Resource.Type#UNDEFINED} since the content is no longer present.
 * </ul>
 *
 * @author Jody Garnett (Boundless)
 */
public class ResourceNotification implements Serializable {

    private static final long serialVersionUID = 1689657047251329584L;

    /** Event kind for the purpose of identification */
    public enum Kind {
        /** Resource created */
        ENTRY_CREATE,
        /** Resource deleted */
        ENTRY_DELETE,
        /** Resource modified */
        ENTRY_MODIFY
    }
    /** Event for resource change notification. */
    public static class Event implements Serializable {
        private static final long serialVersionUID = 2852962095949861322L;
        final String path;
        final Kind kind;

        public Event(String path, Kind kind) {
            this.path = path;
            this.kind = kind;
        }
        /**
         * Nature of change
         *
         * @return nature of change
         */
        public Kind getKind() {
            return kind;
        }
        /**
         * Resource path changed
         *
         * @return resource path changed
         */
        public String getPath() {
            return path;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((kind == null) ? 0 : kind.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Event other = (Event) obj;
            if (kind != other.kind) return false;
            if (path == null) {
                if (other.path != null) return false;
            } else if (!path.equals(other.path)) return false;
            return true;
        }

        @Override
        public String toString() {
            return "Event [path=" + path + ", kind=" + kind + "]";
        }
    }
    /** ResourceStore used for for resource lookup. */
    // final private ResourceStore store;

    /** Notificaiton context */
    private final String path;

    /** Notification kind */
    private final Kind kind;

    /** Delta of changes detected (since last check). */
    private final List<Event> delta;

    private final long timestamp;

    public static List<Event> delta(
            File baseDirectory, List<File> created, List<File> removed, List<File> modified) {
        if (created == null) {
            created = Collections.emptyList();
        }
        if (removed == null) {
            removed = Collections.emptyList();
        }
        if (modified == null) {
            modified = Collections.emptyList();
        }
        int size = created.size() + removed.size() + modified.size();
        if (size == 0) {
            return null;
        }
        List<Event> delta = new ArrayList<Event>(size);
        for (File file : created) {
            String newPath = Paths.convert(baseDirectory, file);
            delta.add(new Event(newPath, Kind.ENTRY_CREATE));
        }
        for (File file : removed) {
            String deletePath = Paths.convert(baseDirectory, file);
            delta.add(new Event(deletePath, Kind.ENTRY_DELETE));
        }
        for (File file : modified) {
            String changedPath = Paths.convert(baseDirectory, file);
            delta.add(new Event(changedPath, Kind.ENTRY_MODIFY));
        }
        return delta;
    }
    /**
     * Notification of a change to a single resource.
     *
     * @param path resource path changed
     * @param kind nature of change
     * @param timestamp local time stamp of change
     */
    ResourceNotification(String path, Kind kind, long timestamp) {
        this.path = path;
        this.kind = kind;
        this.delta = Collections.emptyList();
        this.timestamp = timestamp;
    }

    /**
     * Notification changes to directory contents.
     *
     * @param path resource path changed
     * @param kind nature of change
     * @param timestamp local time stamp of change
     * @param delta List of changes
     */
    @SuppressWarnings("unchecked")
    public ResourceNotification(String path, Kind kind, long timestamp, List<Event> delta) {
        this.path = path;
        this.kind = kind;
        this.delta =
                (List<Event>)
                        (delta != null
                                ? Collections.unmodifiableList(delta)
                                : Collections.emptyList());
        this.timestamp = timestamp;
    }

    public Kind getKind() {
        return kind;
    }

    public String getPath() {
        return path;
    }

    public long getTimestamp() {
        return timestamp;
    }
    /**
     * Paths of changed resources.
     *
     * <p>This list of changed resources is sorted and includes any relevant directories.
     *
     * @return paths of changed resources
     */
    public List<Event> events() {
        return delta; // unmodifiable
    }

    @Override
    public String toString() {
        return "ResourceNotification [path="
                + path
                + ", kind="
                + kind
                + ", delta="
                + delta
                + ", timestamp="
                + timestamp
                + "]";
    }
}
