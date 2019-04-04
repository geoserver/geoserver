/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.server;

import javax.jms.ConnectionFactory;
import javax.jms.Topic;
import org.geoserver.cluster.JMSApplicationListener;
import org.geoserver.cluster.JMSFactory;
import org.geoserver.cluster.events.ToggleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;

/**
 * JMS MASTER (Producer) Listener used to provide basic functionalities to the producer
 * implementations
 *
 * @see {@link JMSApplicationListener}
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public abstract class JMSAbstractProducer extends JMSApplicationListener {

    @Autowired public JMSFactory jmsFactory;

    /** @return the jmsTemplate */
    public final JmsTemplate getJmsTemplate() {
        final ConnectionFactory cf = jmsFactory.getConnectionFactory(config.getConfigurations());
        if (cf == null) {
            throw new IllegalStateException("Unable to load a connectionFactory");
        }
        return new JmsTemplate(cf);
    }

    public final Topic getTopic() {
        final Topic jmsTopic = jmsFactory.getTopic(config.getConfigurations());
        if (jmsTopic == null) {
            throw new IllegalStateException("Unable to load a JMS destination");
        }
        return jmsTopic;
    }

    /**
     * Constructor
     *
     * @param topicTemplate the getJmsTemplate() object used to send message to the topic queue
     */
    public JMSAbstractProducer() {
        super(ToggleType.MASTER);
    }
}
