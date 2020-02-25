/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Logger;
import javax.jms.JMSException;
import javax.jms.Topic;
import org.geoserver.cluster.message.JMSObjectMessageCreator;
import org.geotools.util.logging.Logging;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

/**
 * JMS MASTER (Producer)
 *
 * <p>Class which define a general purpose producer which sends valid ObjectMessages using a
 * JMSTemplate. Valid means that we are appending to the message some conventional (to this JMS
 * plug-in) properties which can be used to synchronize consumer and producers.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public class JMSPublisher {

    static final Logger LOGGER = Logging.getLogger(JMSPublisher.class);

    private final JMSManager jmsManager;

    /** Constructor */
    public JMSPublisher(JMSManager jmsManager) {
        this.jmsManager = jmsManager;
    }

    /**
     * Used to publish the event on the queue.
     *
     * @param <S> a serializable object
     * @param <O> the object to serialize using a JMSEventHandler
     * @param jmsTemplate the template to use to publish on the topic <br>
     *     (default destination should be already set)
     * @param props the JMSProperties used by this instance of GeoServer
     * @param object the object (or event) to serialize and send on the JMS topic
     */
    public <S extends Serializable, O> void publish(
            final Topic destination,
            final JmsTemplate jmsTemplate,
            final Properties props,
            final O object)
            throws JMSException {
        try {

            final JMSEventHandler<S, O> handler = jmsManager.getHandler(object);

            // set the used SPI
            props.put(JMSEventHandlerSPI.getKeyName(), handler.getGeneratorClass().getSimpleName());

            // TODO make this configurable
            final MessageCreator creator =
                    new JMSObjectMessageCreator(handler.serialize(object), props);

            jmsTemplate.send(destination, creator);

        } catch (Exception e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) {
                LOGGER.severe(e.getLocalizedMessage());
            }
            final JMSException ex = new JMSException(e.getLocalizedMessage());
            ex.initCause(e);
            throw ex;
        }
    }
}
