/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.ToggleConfiguration;
import org.geoserver.cluster.events.ToggleEvent;
import org.geoserver.cluster.events.ToggleType;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * This class make it possible to enable and disable the Event producer/consumer using Applications
 * Events.
 *
 * <p>This is used at the GeoServer startup to disable the producer until the initial configuration
 * is loaded.
 *
 * <p>It can also be used to enable/disable the producer in a Master+Slave configuration to avoid
 * recursive event production.
 *
 * @see {@link ToggleEvent}
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSApplicationListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger LOGGER = Logging.getLogger(JMSApplicationListener.class);

    protected final ToggleType type;

    /**
     * This will be set to false:<br>
     * - until the GeoServer context is initialized<br>
     * - if this instance of geoserver act as pure slave
     */
    private volatile Boolean status = false;

    @Autowired public JMSConfiguration config;

    public JMSApplicationListener(ToggleType type) {
        this.type = type;
    }

    @PostConstruct
    private void init() {
        setStatus(getStatus(type, config));
    }

    public static boolean getStatus(final ToggleType type, JMSConfiguration config) {
        Object statusObj;
        if (type.equals(ToggleType.SLAVE)) {
            statusObj = config.getConfiguration(ToggleConfiguration.TOGGLE_SLAVE_KEY);
            if (statusObj == null) {
                statusObj = ToggleConfiguration.DEFAULT_SLAVE_STATUS;
            }
        } else {
            statusObj = config.getConfiguration(ToggleConfiguration.TOGGLE_MASTER_KEY);
            if (statusObj == null) {
                statusObj = ToggleConfiguration.DEFAULT_MASTER_STATUS;
            }
        }
        return Boolean.parseBoolean(statusObj.toString());
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Incoming event of type " + event.getClass().getSimpleName());
        }

        if (event instanceof ToggleEvent) {

            // enable/disable the producer
            final ToggleEvent tEv = (ToggleEvent) event;
            if (tEv.getType().equals(this.type)) {
                setStatus(tEv.toggleTo());
            }

        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "Incoming application event of type " + event.getClass().getSimpleName());
            }
        }
    }

    /** @return the status */
    public final boolean isEnabled() {
        return status;
    }

    /**
     * @param status enable or disable producer
     * @note thread safe
     */
    public final void setStatus(final boolean producerEnabled) {
        if (producerEnabled) {
            // if produce is disable -> enable it
            if (!this.status) {
                synchronized (this.status) {
                    if (!this.status) {
                        this.status = true;
                    }
                }
            }
        } else {
            // if produce is Enabled -> disable
            if (this.status) {
                synchronized (this.status) {
                    if (this.status) {
                        this.status = false;
                    }
                }
            }
        }
    }
}
