/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.client;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.geoserver.cluster.JMSApplicationListener;
import org.geoserver.cluster.JMSFactory;
import org.geoserver.cluster.configuration.ConnectionConfiguration;
import org.geoserver.cluster.configuration.ConnectionConfiguration.ConnectionConfigurationStatus;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.TopicConfiguration;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jms.JmsException;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * Connection handler
 *
 * @author Carlo Cancellieri - GeoSolutions SAS
 */
public final class JMSContainer extends DefaultMessageListenerContainer
        implements DisposableBean,
                ApplicationListener<ContextRefreshedEvent>,
                ApplicationContextAware {

    private static final Logger LOGGER = Logging.getLogger(JMSContainer.class);

    @Autowired public JMSFactory jmsFactory;

    @Autowired
    public List<JMSContainerHandlerExceptionListener> jmsContainerHandleExceptionListener;

    private JMSConfiguration config;

    private boolean verified = false;

    private ApplicationContext applicationContext;

    // times to test (connection)
    private static int max;

    // millisecs to wait between tests (connection)
    private static long maxWait;

    public JMSContainer(JMSConfiguration config, JMSApplicationListener listener) {
        super();

        // configuration
        this.config = config;

        // the listener used to handle incoming events
        setMessageListener(listener);
    }

    @PostConstruct
    private void init() {
        // change the default autostartup status to false
        setAutoStartup(false);

        // force no concurrent consumers
        setConcurrentConsumers(1);

        // set to topic
        setPubSubDomain(true);

        // set subscription durability
        setSubscriptionDurable(
                Boolean.parseBoolean(
                        config.getConfiguration(TopicConfiguration.DURABLE_KEY).toString()));

        // set subscription ID
        setDurableSubscriptionName(
                config.getConfiguration(JMSConfiguration.INSTANCE_NAME_KEY).toString());

        // times to test (connection)
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
        // configure (needed by initializeBean)
        configure();
    }

    private static void verify(final Object type, final String message) {
        if (type == null)
            throw new IllegalArgumentException(
                    message != null ? message : "Verify fails the argument check");
    }

    /**
     * try to disconnect
     *
     * @return true if success
     */
    public boolean disconnect() {
        if (isRunning()) {
            LOGGER.info("Disconnecting...");
            stop();
            for (int rep = 1; rep <= max; ++rep) {
                LOGGER.info("Unregistering...");
                if (!isRunning()) {
                    LOGGER.info("Succesfully un-registered from the destination topic");
                    LOGGER.warning(
                            "You will (probably) loose next incoming events from other instances!!! (depending on how you have configured the broker)");
                    return true;
                }
                LOGGER.info("Waiting for connection shutdown...(" + rep + "/" + max + ")");
                try {
                    Thread.sleep(maxWait);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        } else {
            LOGGER.severe("Connection is already stopped");
        }

        return false;
    }

    private void configure() {
        final Properties conf = config.getConfigurations();

        // set destination
        setDestination(jmsFactory.getClientDestination(conf));

        // use a CachingConnectionFactory
        setConnectionFactory(jmsFactory.getConnectionFactory(conf));
    }

    /**
     * try to connect
     *
     * @return true in success case, false otherwise
     */
    public boolean connect() {
        if (!isRunning()) {
            LOGGER.info("Connecting...");
            start();
            if (isRunning()) {
                for (int repReg = 1; repReg <= max; ++repReg) {
                    LOGGER.info("Registration...");
                    if (isRegisteredWithDestination()) {
                        LOGGER.info("Now GeoServer is registered with the destination");
                        return true;
                    } else if (repReg == max) {
                        LOGGER.log(
                                Level.SEVERE, "Registration aborted due to a connection problem");
                        stop();
                        LOGGER.info("Disconnected");
                    } else {
                        LOGGER.info(
                                "Impossible to register GeoServer with destination, waiting...");
                    }
                    LOGGER.info("Waiting for registration...(" + repReg + "/" + max + ")");
                    try {
                        Thread.sleep(maxWait);
                    } catch (InterruptedException e) {
                        LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
                    }
                }
            } else {
                LOGGER.severe("Impossible to start a connection to destination.");
                stop();
                LOGGER.info("Disconnected");
                return false;
            }
        } else {
            LOGGER.severe("Connection is already running");
        }
        return false;
    }

    @Override
    public void start() throws JmsException {
        if (!verified) {
            verify(jmsFactory, "failed to get a JMSFactory");
            verified = true;
        }
        if (!isRunning()) {

            // configure the container
            configure();

            // start it
            super.start();

            // initialize the container
            initialize();
        }
    }

    @Override
    public void destroy() {
        super.stop();
        super.destroy();
    }

    @Override
    protected void handleListenerSetupFailure(Throwable ex, boolean alreadyRecovered) {
        super.handleListenerSetupFailure(ex, alreadyRecovered);

        if (jmsContainerHandleExceptionListener != null) {
            for (JMSContainerHandlerExceptionListener handler :
                    jmsContainerHandleExceptionListener) {
                handler.handleListenerSetupFailure(ex, alreadyRecovered);
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() == applicationContext) {
            final String startString =
                    config.getConfiguration(ConnectionConfiguration.CONNECTION_KEY);
            if (startString != null
                    && startString.equals(ConnectionConfigurationStatus.enabled.toString())) {
                if (!connect()) {
                    if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.severe(
                                "Unable to connect to the broker, force connection status to disabled");
                    }

                    // change configuration status
                    config.putConfiguration(
                            ConnectionConfiguration.CONNECTION_KEY,
                            ConnectionConfigurationStatus.disabled.toString());

                    // store changes to the configuration
                    try {
                        config.storeConfig();
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        }
    }
}
