/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import org.apache.commons.io.IOUtils;

/**
 * Jetty starter, will run GeoBatch inside the Jetty web container.<br>
 * Useful for debugging, especially in IDE were you have direct dependencies between the sources of
 * the various modules (such as Eclipse).
 *
 * @author Andrea Aime - GeoSolutions SAS
 * @author Carlo Cancellieri - GeoSolutions SAS
 */
public class Start {
    private static final Logger log = LoggerFactory.getLogger(Start.class);

    public static void main(String[] args) {
        // don't even think of serving more than XX requests in parallel...
        // we have a limit in our processing and memory capacities
        ThreadPoolExecutor tp = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        tp.setMaximumPoolSize(50);

        Server server = null;
        ServerConnector conn = null;
        try {
            server = new Server(new ExecutorThreadPool(tp));

            // TODO pass properties file
            File properties = null;
            if (args.length == 1) {
                String propertiesFileName = args[0];
                if (!propertiesFileName.isEmpty()) {
                    properties = new File(propertiesFileName);
                }
            } else {
                properties = new File("src/test/resources/jetty.properties");
            }
            Properties prop = loadProperties(properties);

            for (Object key : prop.keySet()) {
                String property = System.getProperty(key.toString());
                String envProp = System.getenv(key.toString());
                if (property != null) {
                    prop.put(key, property);
                } else if (envProp != null) {
                    prop.put(key, envProp);
                }
            }

            // load properties into system env
            setSystemProperties(prop);

            server.setHandler(configureContext(prop));

            conn = configureConnection(prop, server);

            server.setConnectors(new Connector[] {conn});

            server.start();

            // use this to test normal stop behavior, that is, to check stuff
            // that
            // need to be done on container shutdown (and yes, this will make
            // jetty stop just after you started it...)
            // jettyServer.stop();
        } catch (Throwable e) {
            log.error("Could not start the Jetty server: " + e.getMessage(), e);

            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e1) {
                    log.error("Unable to stop the Jetty server:" + e1.getMessage(), e1);
                }
            }
            if (conn != null) {
                try {
                    conn.stop();
                } catch (Exception e1) {
                    log.error("Unable to stop the connection:" + e1.getMessage(), e1);
                }
            }
        }
    }

    public static final String JETTY_PORT = "jetty.port";

    public static final String JETTY_PORT_DEFAULT = "8080";

    private static Properties loadProperties(final File props)
            throws IllegalArgumentException, IOException {
        Properties prop = new Properties();
        if (props == null || !props.exists()) {
            throw new IllegalArgumentException("Bad file name argument: " + props);
        }
        FileInputStream is = null;
        try {
            is = new FileInputStream(props);
            prop.load(is);
        } finally {
            if (is != null) is.close();
        }

        return prop;
    }

    private static int parseInt(String portVariable) {
        if (portVariable == null) {
            return -1;
        }

        try {
            return Integer.valueOf(portVariable).intValue();
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static ServerConnector configureConnection(final Properties prop, Server server) {

        ServerConnector conn = new ServerConnector(server);

        conn.setPort(parseInt(prop.getProperty(JETTY_PORT, JETTY_PORT_DEFAULT)));
        conn.setAcceptQueueSize(100);

        return conn;
    }

    public static final String CONTEXT_PATH = "context.path";

    public static final String CONTEXT_PATH_DEFAULT = "/geobatch";

    public static final String WAR_PATH = "war.path";

    public static final String WAR_PATH_DEFAULT = "src/main/webapp";

    public static final String TEMP_DIR = "temp.dir";

    public static final String TEMP_DIR_DEFAULT = "target/work";

    private static WebAppContext configureContext(final Properties prop) {
        WebAppContext wah = new WebAppContext();

        wah.setContextPath(prop.getProperty(CONTEXT_PATH, CONTEXT_PATH_DEFAULT));
        wah.setWar(prop.getProperty(WAR_PATH, WAR_PATH_DEFAULT));
        wah.setTempDirectory(new File(prop.getProperty(TEMP_DIR, TEMP_DIR_DEFAULT)));
        return wah;
    }

    private static void setSystemProperties(final Properties prop) {
        Iterator<?> it = prop.keySet().iterator();
        while (it.hasNext()) {
            Object key = it.next();
            System.setProperty(key.toString(), prop.get(key).toString());
        }
    }
}
