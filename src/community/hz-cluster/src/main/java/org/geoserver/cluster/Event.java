/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import com.google.common.base.Objects;
import java.io.Serializable;
import java.util.UUID;

/**
 * Base class for events to be signalled across the cluster. Carries an identifier for the
 * originating node which will be implementation specific.
 */
public class Event implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    Serializable source;

    private UUID uuid;

    public Event() {
        this.uuid = UUID.randomUUID();
    }

    public UUID getUUID() {
        return uuid;
    }

    /** Set an identifier for the node on which the event originates. */
    public void setSource(Serializable source) {
        this.source = source;
    }

    /** Get an identifier of the node on which the event originated. */
    public Serializable getSource() {
        return source;
    }

    @Override
    public String toString() {
        return new StringBuilder("Event[").append(source).append(']').toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Event.class, source);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Event)) {
            return false;
        }
        return Objects.equal(source, ((Event) o).source);
    }
}
