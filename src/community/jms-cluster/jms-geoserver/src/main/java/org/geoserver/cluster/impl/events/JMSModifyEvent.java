/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.events;

import java.io.Serializable;
import java.util.List;
import org.geoserver.cluster.impl.events.configuration.JMSEventType;

/**
 * Class implementing a generic JMS Modify event.<br>
 * It is used to handle serialization of a modify event.<br>
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * @param <S> a Serializable object
 */
public class JMSModifyEvent<S extends Serializable> {

    private final List<String> propertyNames;

    private final List<Object> oldValues;

    private final List<Object> newValues;

    private final S source;

    // identifies the type of event (added, removed or modified)
    private final JMSEventType eventType;

    public JMSModifyEvent(
            final S source,
            final List<String> propertyNames,
            final List<Object> oldValues,
            final List<Object> newValues) {
        this.source = source;
        this.propertyNames = propertyNames;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.eventType = JMSEventType.MODIFIED;
    }

    public JMSModifyEvent(
            final S source,
            final List<String> propertyNames,
            final List<Object> oldValues,
            final List<Object> newValues,
            JMSEventType eventType) {
        this.source = source;
        this.propertyNames = propertyNames;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.eventType = eventType;
    }

    /** @return the propertyNames */
    public final List<String> getPropertyNames() {
        return propertyNames;
    }

    /** @return the oldValues */
    public final List<Object> getOldValues() {
        return oldValues;
    }

    /** @return the newValues */
    public final List<Object> getNewValues() {
        return newValues;
    }

    public S getSource() {
        return (S) source;
    }

    /** */
    private static final long serialVersionUID = 1L;

    public JMSEventType getEventType() {
        return eventType;
    }
}
