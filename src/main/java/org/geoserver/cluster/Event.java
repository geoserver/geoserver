package org.geoserver.cluster;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Objects;

/**
 * Base class for events to be signalled across the cluster.  Carries an identifier for the 
 * originating node which will be implementation specific.
 */
public class Event implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    
    Serializable source;
    
    /**
     * Set an identifier for the node on which the event originates.
     * @param source
     */
    public void setSource(Serializable source) {
        this.source = source;
    }

    /**
     * Get an identifier of the node on which the event originated.
     * @param source
     */
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
