/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Topic;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.geoserver.cluster.JMSFactory;
import org.geoserver.cluster.configuration.BrokerConfiguration;
import org.geoserver.cluster.configuration.ConnectionConfiguration;
import org.geoserver.cluster.configuration.ConnectionConfiguration.ConnectionConfigurationStatus;
import org.geoserver.cluster.configuration.EmbeddedBrokerConfiguration;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.TopicConfiguration;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/** @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it */
public class JMSActiveMQFactory extends JMSFactory
        implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {

    private static final java.util.logging.Logger LOGGER =
            Logging.getLogger(JMSActiveMQFactory.class);

    @Autowired private JMSConfiguration config;

    @Autowired private JMSXBeanBrokerFactory bf;

    // used to track changes to the configuration
    private String brokerURI;

    private String topicName;

    private PooledConnectionFactory cf;

    private Topic topic;

    // times to test (connection)
    private static int max;

    // millisecs to wait between tests (connection)
    private static long maxWait;

    // embedded brokerURI
    private BrokerService brokerService;

    private String brokerName;

    private ApplicationContext applicationContext;

    // <bean id="JMSClientDestination"
    // class="org.apache.activemq.command.ActiveMQQueue">
    // <value="Consumer.${instance.name}.VirtualTopic.${topic.name}" />
    // </bean>
    @Override
    public Destination getClientDestination(Properties configuration) {
        StringBuilder builder = new StringBuilder("Consumer.");
        String instanceName = configuration.getProperty(JMSConfiguration.INSTANCE_NAME_KEY);
        String topicName = configuration.getProperty(TopicConfiguration.TOPIC_NAME_KEY);
        return new org.apache.activemq.command.ActiveMQQueue(
                builder.append(instanceName).append(".").append(topicName).toString());
    }

    // <!-- DESTINATION -->
    // <!-- A Destination in ActiveMQ -->
    // <bean id="JMSServerDestination"
    // class="org.apache.activemq.command.ActiveMQTopic">
    // <!-- <constructor-arg value="VirtualTopic.${topic.name}" /> -->
    // <constructor-arg value="VirtualTopic.>" />
    // </bean>
    @Override
    public Topic getTopic(Properties configuration) {
        // TODO move me to implementation jmsFactory implementation
        // if topicName is changed
        final String topicConfiguredName =
                configuration.getProperty(TopicConfiguration.TOPIC_NAME_KEY);
        if (topic == null || topicName.equals(topicConfiguredName)) {
            topicName = topicConfiguredName;
            topic =
                    new org.apache.activemq.command.ActiveMQTopic(
                            configuration.getProperty(TopicConfiguration.TOPIC_NAME_KEY));
        }
        return topic;
    }

    // <!-- A connection to ActiveMQ -->
    @Override
    public ConnectionFactory getConnectionFactory(Properties configuration) {
        final String _brokerURI = config.getConfiguration(BrokerConfiguration.BROKER_URL_KEY);
        final boolean changed = checkBrokerURI(_brokerURI);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Using brokerURI: " + brokerURI);
        }

        if (cf == null) {
            // need to be initialized
            cf = new PooledConnectionFactory(brokerURI);

        } else {
            if (changed) {
                // clear pending connections
                try {
                    destroyConnectionFactory();
                } catch (Exception e) {
                    // eat
                }
                // create a new connection
                cf = new PooledConnectionFactory(brokerURI);
                // cf.start();
            }
        }
        return cf;
    }

    /** @return true if brokerURI is modified */
    private boolean checkBrokerURI(final String _brokerURI) {
        if (brokerURI == null) {
            if (_brokerURI == null || _brokerURI.length() == 0) {
                brokerURI = getDefaultURI();
                return true;
            } else {
                // initialize to the new configuration
                brokerURI = _brokerURI;

                // check the URI syntax
                try {
                    new URI(brokerURI);
                } catch (URISyntaxException e) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.severe(e.getLocalizedMessage());
                    }
                    brokerURI = getDefaultURI();
                }
                return true;
            }
        } else {
            if (_brokerURI == null || _brokerURI.length() == 0) {
                // use the default
                return checkBrokerURI(getDefaultURI());

            } else if (brokerURI.equalsIgnoreCase(_brokerURI)) {
                return false;
            } else {
                // something is changed: reset
                brokerURI = null;
                return checkBrokerURI(_brokerURI);
            }
        }
    }

    private String getDefaultURI() {
        // default brokerURI
        return "vm://"
                + config.getConfiguration(JMSConfiguration.INSTANCE_NAME_KEY)
                + "?create=false&waitForStart=5000";
    }

    private void destroyConnectionFactory() {
        if (cf != null) {
            // close all the connections
            cf.clear();
            // stop the factory
            cf.stop();
            // set null
            cf = null;
        }
    }

    private void destroyBrokerService() throws Exception {
        if (brokerService != null) {
            brokerService.stop();
        }
    }

    @Override
    public void destroy() throws Exception {
        destroyConnectionFactory();
        destroyBrokerService();
    }

    @Override
    public boolean startEmbeddedBroker(final Properties configuration) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Starting the embedded brokerURI");
        }
        if (brokerService == null) {
            final String xBeanBroker =
                    configuration.getProperty(ActiveMQEmbeddedBrokerConfiguration.BROKER_URL_KEY);
            // final XBeanBrokerFactory bf = new XBeanBrokerFactory();
            brokerService = bf.createBroker(new URI(xBeanBroker));
            brokerService.setEnableStatistics(false);

            // override the name of the brokerURI using the instance name which
            // should be unique within the network
            brokerName = configuration.getProperty(JMSConfiguration.INSTANCE_NAME_KEY);
            brokerService.setBrokerName(brokerName);
            brokerService.setUseLocalHostBrokerName(false);
            brokerService.setVmConnectorURI(new URI("vm://" + brokerName));
        } else {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning(
                        "The embedded brokerURI service already exists, probably it is already started");
            }
            if (brokerService.isStarted()) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("SKIPPING: The embedded brokerURI is already started");
                }
                return true;
            }
        }
        if (!brokerService.isStarted()) {
            brokerService.start();
        }
        for (int i = -1; i < max; ++i) {
            try {
                if (brokerService.isStarted()) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Embedded brokerURI is now started");
                    }
                    return true;
                }
                Thread.sleep(maxWait);
            } catch (Exception e1) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe(
                            "Unable to start the embedded brokerURI" + e1.getLocalizedMessage());
                }
                return false;
            }
        }
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.severe("Unable to start the embedded brokerURI");
        }
        return false;
    }

    @Override
    public boolean isEmbeddedBrokerStarted() {
        return brokerService == null ? false : brokerService.isStarted();
    }

    @Override
    public boolean stopEmbeddedBroker() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Embedded brokerURI is now stopped");
        }
        if (brokerService == null) {
            return true;
        }
        brokerService.stop();
        for (int i = -1; i < max; ++i) {
            try {
                if (!brokerService.isStarted()) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info("Embedded brokerURI is now stopped");
                    }
                    brokerService = null;
                    return true;
                }
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info("Embedded brokerURI is going to stop: waiting...");
                }
                Thread.sleep(maxWait);
            } catch (Exception e1) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.severe(
                            "Unable to start the embedded brokerURI" + e1.getLocalizedMessage());
                }
                return false;
            }
        }
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.severe("Unable to stop the embedded brokerURI");
        }
        return false;
    }

    private void init() {
        // // times to test (connection)
        max =
                Integer.parseInt(
                        config.getConfiguration(ConnectionConfiguration.CONNECTION_RETRY_KEY)
                                .toString());
        // millisecs to wait between tests (connection)
        maxWait =
                Long.parseLong(
                        config.getConfiguration(ConnectionConfiguration.CONNECTION_MAXWAIT_KEY)
                                .toString());

        // check configuration for connection and try to start if needed
        if (EmbeddedBrokerConfiguration.isEnabled(config)) {
            if (!isEmbeddedBrokerStarted()) {
                try {
                    if (!startEmbeddedBroker(config.getConfigurations())) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.severe(
                                    "Unable to start the embedded brokerURI, force status to disabled");
                        }

                        // change configuration status
                        config.putConfiguration(
                                ConnectionConfiguration.CONNECTION_KEY,
                                ConnectionConfigurationStatus.disabled.toString());

                    } else {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.info(
                                    "Started the embedded brokerURI: " + brokerService.toString());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    // change configuration status
                    config.putConfiguration(
                            ConnectionConfiguration.CONNECTION_KEY,
                            ConnectionConfigurationStatus.disabled.toString());
                }
                // store changes to the configuration
                try {
                    config.storeConfig();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            } else {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("The brokerURI seems to be already started");
                }
            }
        }
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() == applicationContext) {
            init();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
