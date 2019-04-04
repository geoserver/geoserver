/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.configuration;

import com.thoughtworks.xstream.XStream;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.cluster.JMSEventHandlerSPI;
import org.geoserver.cluster.events.ToggleSwitch;
import org.geoserver.cluster.impl.events.configuration.JMSSettingsModifyEvent;
import org.geoserver.config.GeoServer;

/** SPI class used by the JMS manager to instantiate the proper handler for a certain event type. */
public final class JMSSettingsHandlerSPI
        extends JMSEventHandlerSPI<String, JMSSettingsModifyEvent> {

    private final GeoServer geoserver;
    private final XStream xstream;
    private final ToggleSwitch producer;

    public JMSSettingsHandlerSPI(
            int priority, GeoServer geo, XStream xstream, ToggleSwitch producer) {
        super(priority);
        this.geoserver = geo;
        this.xstream = xstream;
        this.producer = producer;
    }

    @Override
    public boolean canHandle(Object event) {
        // we can handle only settings modified events
        return event instanceof JMSSettingsModifyEvent;
    }

    @Override
    public JMSEventHandler<String, JMSSettingsModifyEvent> createHandler() {
        return new JMSSettingsHandler(geoserver, xstream, this.getClass(), producer);
    }
}
