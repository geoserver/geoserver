/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.events.configuration;

import java.util.Collections;
import java.util.List;
import org.geoserver.cluster.impl.events.JMSModifyEvent;
import org.geoserver.config.SettingsInfo;

/**
 * This class defines an event for modified settings. This settings represents metadata information
 * for the GeoServer instance or for a specific workspace. By default an workspace doesn't have any
 * settings and use GeoServer global settings
 *
 * <p>
 *
 * <p>A settings modified event can represent three situations, the settings were added, removed or
 * the settings were modified.
 */
public class JMSSettingsModifyEvent extends JMSModifyEvent<SettingsInfo> {

    private static final long serialVersionUID = 1L;

    public JMSSettingsModifyEvent(
            SettingsInfo source,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        this(source, propertyNames, oldValues, newValues, JMSEventType.MODIFIED);
    }

    public JMSSettingsModifyEvent(SettingsInfo source, JMSEventType eventType) {
        this(
                source,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                eventType);
    }

    public JMSSettingsModifyEvent(
            SettingsInfo source,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues,
            JMSEventType eventType) {
        super(source, propertyNames, oldValues, newValues, eventType);
    }
}
