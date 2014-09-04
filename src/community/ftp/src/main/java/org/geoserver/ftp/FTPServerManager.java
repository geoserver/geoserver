/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ftp;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ftpserver.DataConnectionConfiguration;
import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;

/**
 * Starts and stop the FTP server.
 * 
 * @author Andrea Aime - OpenGeo
 */
public class FTPServerManager implements ApplicationListener {

    static final Logger LOGGER = Logging.getLogger(FTPServerManager.class);

    private FtpServer ftp;

    private UserManager userManager;

    private FtpLetFinder callbacks;

    private FTPConfigLoader loader;

    private FTPConfig config;

    /**
     * Sets up the {@link FtpServer FTP Server} managed by this bean using the provided
     * {@code userManager}
     * 
     * @param userManager
     */
    public FTPServerManager(final UserManager userManager, FtpLetFinder callbacks,
            FTPConfigLoader loader) {
        this.userManager = userManager;
        this.callbacks = callbacks;
        this.loader = loader;
        this.config = loader.load();
        configureServer();
    }

    /**
     * Creates the FTP server and sets up the FTP listeners by looking up the application context
     * for instances of the {@link FTPCallback} extension point.
     */
    private void configureServer() {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory listenerFactory = new ListenerFactory();
        DataConnectionConfigurationFactory dataConnConfigFac = new DataConnectionConfigurationFactory();

        DataConnectionConfiguration connectionConfiguration = dataConnConfigFac
                .createDataConnectionConfiguration();

        LOGGER.info("Configuring GeoServer's FTP Server...");
        String passivePorts = config.getPassivePorts();
        if (passivePorts != null && passivePorts.trim().length() > 0) {
            try {
                LOGGER.info("Setting FTP passive ports: " + passivePorts
                        + ". May take a few seconds while checking if they're already bound.");
                dataConnConfigFac.setPassivePorts(passivePorts);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error setting the FTP server passive ports, "
                        + "check the ftp.xml config file. Format is '\"\"|<minPort[-maxPort]>  ", e);
            }
        }
        String passiveAddress = config.getPassiveAddress();
        if (passiveAddress == null || passiveAddress.trim().length() == 0
                || FTPConfig.DEFAULT_PASSIVE_ADDRESS.equals(passiveAddress)) {
            LOGGER.info("Passive address is the default server address");
        } else {
            LOGGER.info("Passive address: " + passiveAddress);
            dataConnConfigFac.setPassiveAddress(passiveAddress);
        }
        String pasvExternalAddress = config.getPassiveExternalAddress();
        if (pasvExternalAddress == null || pasvExternalAddress.trim().length() == 0
                || FTPConfig.DEFAULT_PASSIVE_ADDRESS.equals(pasvExternalAddress)) {
            LOGGER.info("Passive external address is the default server address");
        } else {
            LOGGER.info("Passive external address: " + pasvExternalAddress);
            dataConnConfigFac.setPassiveExternalAddress(pasvExternalAddress);
        }

        // configure a listener on port 8021
        LOGGER.info("FTP port: " + config.getFtpPort());
        listenerFactory.setPort(config.getFtpPort());

        LOGGER.info("Iddle timeout: " + config.getIdleTimeout() + "s");
        listenerFactory.setIdleTimeout(config.getIdleTimeout());

        String serverAddress = config.getServerAddress();
        if (serverAddress == null || serverAddress.trim().length() > 0
                || serverAddress.toLowerCase().equals(FTPConfig.ALL_SERVER_ADDRESSES_FLAG)) {
            LOGGER.info("Bound to all available network interfaces");
        } else {
            LOGGER.info("Bound to server address: " + serverAddress);
            listenerFactory.setServerAddress(config.getServerAddress());
        }

        listenerFactory.setDataConnectionConfiguration(connectionConfiguration);
        serverFactory.addListener("default", listenerFactory.createListener());

        // link the server user management to the GS one
        serverFactory.setUserManager(userManager);

        // find out the listeners
        Map<String, Ftplet> ftplets = callbacks.getFtpLets();
        LOGGER.info("FTPLet callbacks: " + ftplets);
        serverFactory.setFtplets(ftplets);

        // start the server
        ftp = serverFactory.createServer();
    }

    public void startServer() throws FtpException {
        if (!config.isEnabled()) {
            return;
        }
        if (ftp.isStopped() || ftp.isSuspended()) {
            LOGGER.info("Starting GeoServer FTP Server on port " + config.getFtpPort());
            ftp.start();
            LOGGER.info("GeoServer FTP Server started");
        }
    }

    public void stopServer() {
        if (!ftp.isStopped()) {
            LOGGER.info("Stopping GeoServer FTP Server on port " + config.getFtpPort());
            ftp.stop();
            LOGGER.info("GeoServer FTP Server stopped");
        }
    }

    /**
     * Listens to the application context events in order to automatically start/stop the FTP server
     * upon application startup/shutdown.
     * 
     * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
     * @see ContextRefreshedEvent
     * @see ContextStoppedEvent
     * @see ContextClosedEvent
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            try {
                startServer();
            } catch (FtpException e) {
                LOGGER.log(Level.SEVERE, "Could not start the embedded FTP server", e);
            }
        } else if (event instanceof ContextStoppedEvent || event instanceof ContextClosedEvent) {
            stopServer();
        }
    }

}
