/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.events.configuration;

import java.util.Collections;
import java.util.List;
import org.geoserver.cluster.impl.events.JMSModifyEvent;
import org.geoserver.cluster.impl.handlers.configuration.JMSServiceHandler;
import org.geoserver.config.ServiceInfo;

/**
 * This Class define a wrapper of the {@link JMSModifyEvent} class to define an event which can be
 * recognized by the {@link JMSServiceHandler} as ServiceInfo modified events.
 *
 * <p>A service modified event can represent three situations, the service was added, removed or is
 * configuration was modified.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSServiceModifyEvent extends JMSModifyEvent<ServiceInfo> {

    private static final long serialVersionUID = 1L;

    // identifies the type of event (added, removed or service configuration modified)
    private final JMSEventType eventType;

    public JMSServiceModifyEvent(
            ServiceInfo source,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        this(source, propertyNames, oldValues, newValues, JMSEventType.MODIFIED);
    }

    public JMSServiceModifyEvent(ServiceInfo source, JMSEventType eventType) {
        this(
                source,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                eventType);
    }

    public JMSServiceModifyEvent(
            ServiceInfo source,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues,
            JMSEventType eventType) {
        super(source, propertyNames, oldValues, newValues, eventType);
        this.eventType = eventType;
    }

    public JMSEventType getEventType() {
        return eventType;
    }
}
