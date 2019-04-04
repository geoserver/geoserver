/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import org.geotools.util.logging.Logging;

/**
 * JMS SLAVE (Consumer)
 *
 * <p>Simple Class which shows how to define and use a general purpose synchronizer.
 *
 * @see {@link JMSEventHandler}
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSSynchronizer {

    private static final Logger LOGGER = Logging.getLogger(JMSSynchronizer.class);

    private final JMSManager jmsManager;

    public JMSSynchronizer(JMSManager jmsManager) {
        this.jmsManager = jmsManager;
    }

    public <S extends Serializable, O> void synchronize(final O event, final Properties props)
            throws JMSException, IllegalArgumentException {
        try {
            // try to get the handler from the spring context
            final JMSEventHandler<S, O> handler = jmsManager.getHandler(event);
            // if handler is not found
            if (handler == null) {
                throw new IllegalArgumentException(
                        "Unable to locate a valid handler for the incoming event: " + event);
            }
            // else try to synchronize event using the obtained handler
            handler.synchronize(event);

        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.severe("Unable to synchronize event: " + event + " locally");
            }
            final JMSException ex = new JMSException(e.getLocalizedMessage());
            ex.initCause(e);
            throw ex;
        }
    }
}
