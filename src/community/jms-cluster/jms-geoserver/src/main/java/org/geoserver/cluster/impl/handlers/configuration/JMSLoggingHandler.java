/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.configuration;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.beanutils.BeanUtils;
import org.geoserver.cluster.events.ToggleSwitch;
import org.geoserver.config.GeoServer;
import org.geoserver.config.LoggingInfo;

/**
 * JMS Handler is used to synchronize
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSLoggingHandler extends JMSConfigurationHandler<LoggingInfo> {
    private final GeoServer geoServer;

    private final ToggleSwitch producer;

    public JMSLoggingHandler(GeoServer geo, XStream xstream, Class clazz, ToggleSwitch producer) {
        super(xstream, clazz);
        this.geoServer = geo;
        this.producer = producer;
    }

    @Override
    protected void omitFields(final XStream xstream) {
        // omit not serializable fields
        // NOTHING TO DO
    }

    @Override
    public boolean synchronize(LoggingInfo info) throws Exception {
        if (info == null) {
            throw new NullPointerException("Incoming object is null");
        }
        try {
            // LOCALIZE service
            final LoggingInfo localObject = geoServer.getLogging();
            // overwrite local object members with new incoming settings
            BeanUtils.copyProperties(localObject, info);

            // disable the message producer to avoid recursion
            producer.disable();

            // save the localized object
            geoServer.save(localObject);

        } catch (Exception e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                LOGGER.severe(
                        this.getClass() + " is unable to synchronize the incoming event: " + info);
            throw e;
        } finally {
            // enable message the producer
            producer.enable();
        }
        return true;
    }
}
