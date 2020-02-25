/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.configuration;

import com.thoughtworks.xstream.XStream;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.cluster.JMSEventHandlerSPI;
import org.geoserver.cluster.events.ToggleSwitch;
import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;

public class JMSLoggingHandlerSPI extends JMSEventHandlerSPI<String, LoggingInfo> {

    final GeoServer geoserver;

    final XStream xstream;

    final ToggleSwitch producer;

    public JMSLoggingHandlerSPI(
            final int priority,
            final GeoServer geo,
            final XStream xstream,
            final ToggleSwitch producer) {
        super(priority);
        this.geoserver = geo;
        this.xstream = xstream;
        this.producer = producer;
    }

    @Override
    public boolean canHandle(final Object event) {
        if (event instanceof LoggingInfo) return true;
        else return false;
    }

    @Override
    public JMSEventHandler<String, LoggingInfo> createHandler() {
        return new JMSLoggingHandler(geoserver, xstream, this.getClass(), producer);
    }
}
