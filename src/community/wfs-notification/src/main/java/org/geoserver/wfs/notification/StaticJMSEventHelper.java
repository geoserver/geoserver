/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import static org.geoserver.wfs.notification.Util.coalesce;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.config.GeoServer;

public class StaticJMSEventHelper extends JMSEventHelper {
    private static final String DESTINATION_PREFIX = "destination.";

    private static final Log LOG = LogFactory.getLog(StaticJMSEventHelper.class);

    private GeoServer geoServer;
    private String propertiesFileName;
    private URL propertiesURL;
    private Properties properties;
    private long checkInterval = 10000;
    private AbstractFileWatcher<Properties> propertiesFileWatcher;
    
    private long waitUntil = -1; // Last time getJMSInfo failed; don't try again until this time

    private String connectionFactoryName = "connectionFactory";
    private List<String> destinationNames;

    private JMSInfo info; // Registered by createJMSInfo; we should only have one functioning JMSInfo at a time

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
    }
    
    public void setProperties(Properties properties) {
        if(properties == this.properties)
            return;
        this.properties = properties;
        this.registerJMSConnection(null);
    }

    public void setPropertiesFileName(String propertiesFileName) {
        this.propertiesFileName = propertiesFileName;
    }

    public void setConnectionFactoryName(String connectionFactoryName) {
        this.connectionFactoryName = connectionFactoryName;
    }

    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }

    public void setDestinationNames(List<String> destinationNames) {
        this.destinationNames = destinationNames;
    }
    
    public void init() {
        if(properties == null) {
            if(propertiesFileName == null) {
                throw new IllegalArgumentException("One of properties or propertiesFileName must be supplied to this bean.");
            }
            
            String propertiesFileName = this.propertiesFileName;
            propertiesURL = interpretAsURL(propertiesFileName);
            if(propertiesURL == null) {
                throw new IllegalArgumentException("Couldn't interpret " + propertiesFileName + " as a File, classpath entry, or URL.");
            }
            propertiesFileWatcher = new AbstractFileWatcher<Properties>(checkInterval, null, propertiesURL) {
                protected boolean shouldUpdate(URLConnection conn) {
                    return getData() == null || super.shouldUpdate(conn);
                };
                
                @Override
                protected Properties doLoad(URLConnection conn) throws IOException {
                    try {
                        Properties p = new Properties();
                        p.load(propertiesURL.openStream());
                        return p;
                    } catch(IOException e) {
                        throw e;
                    } catch(Throwable e) {
                        throw new IOException(e);
                    }
                }
            };
        }
    }

    protected URL interpretAsURL(String propertiesFileName) {
        URL propertiesURL = null;

        if(geoServer != null) {
            try {
                File f = geoServer.getCatalog().getResourceLoader().find(propertiesFileName);
                if(f != null && f.canRead()) {
                    propertiesURL = f.toURI().toURL();
                }
            } catch(IOException e) {
            }
        }
        
        {
            File f = new File(propertiesFileName);
            if(f.canRead()) {
                try {
                    propertiesURL = f.toURI().toURL();
                } catch(IOException e) {
                }
            }
        }

        if(propertiesURL == null) {
            propertiesURL = StaticJMSEventHelper.class.getResource(propertiesFileName);
        }

        if(propertiesURL == null) {
            propertiesURL = Thread.currentThread().getContextClassLoader().getResource(propertiesFileName);
        }

        if(propertiesURL == null) {
            try {
                propertiesURL = new URL(propertiesFileName);
            } catch(MalformedURLException e1) {
                // ignore
            }
        }

        return propertiesURL;
    }

    @Override
    public boolean isReady() {
        return getJMSInfo() != null;
    }

    @Override
    synchronized protected JMSInfo getJMSInfo() {
        if(waitUntil > 0 && System.currentTimeMillis() < waitUntil) {
            // Don't try too frequently if it's not working
            return null;
        }
        
        Properties p = null;

        // Get properties either from the bean configuration or from a properties file
        if(propertiesFileWatcher != null) {
            try {
                p = propertiesFileWatcher.load(); // May be cached
                setProperties(p);
            } catch(IOException e) {
                LOG.warn("Unable to load JNDI properties from file " + propertiesFileWatcher.getURL() +
                    " -- continuing with old file.", e);
            }
        } else {
            p = properties;
        }
        
        if(p == null) {
            LOG.warn("No properties available for JMS configuration.");
            return null;
        }
        
        JMSInfo info = this.info;

        if(info == null || info.closed || p != properties) { // Do we need to replace the JMS connection?
            try {
                info = createJMSInfo(p);
            } catch(Throwable e) {
                LOG.warn(
                    "Unable to load JNDI properties from bean definition. Continuing with previous JMS connection.", e);
            }
        }

        registerJMSConnection(info);
        
        if(info == null || info.closed) {
            waitUntil = System.currentTimeMillis() + checkInterval;
        } else {
            waitUntil = -1;
        }
        
        return info;
    }
    
    protected JMSInfo createJMSInfo(Properties p) throws NamingException, JMSException {
        JMSInfo info = null;
        
        Context ctx = new InitialContext(p);
        ConnectionFactory factory =
            (ConnectionFactory) ctx.lookup(coalesce((String)p.getProperty("connectionFactoryName"), connectionFactoryName, "connectionFactory"));
        Connection connection = factory.createConnection();
        
        // Find a list of destination names.
        
        // Try a provided list first
        List<String> destinationNames = this.destinationNames;
        
        if(destinationNames == null) {
            // If none were provided, scan the properties for likely suspects
            destinationNames = new ArrayList<String>();
            for(String e : (((Map<String,String>)(Object)p).keySet())) {
                if(e.startsWith(DESTINATION_PREFIX)) {
                    destinationNames.add(e.substring(DESTINATION_PREFIX.length()));
                }
            }
        }
        
        if(destinationNames.isEmpty()) {
            throw new IllegalArgumentException("No destination names were provided or found.");
        }
        
        List<Destination> destinations = new ArrayList<Destination>(destinationNames.size());
        for(String name : destinationNames) {
            destinations.add((Destination) ctx.lookup(name));
        }
        info = new JMSInfo(connection, destinations);
        
        return info;
    }

    protected void registerJMSConnection(JMSInfo newInfo) {
        if(this.info != null && this.info != newInfo) {
            this.info.close();
        }
        this.info = newInfo;
    }

    public void destroy() {
        this.propertiesFileWatcher = null;
        this.properties = null;
        this.registerJMSConnection(null);
    }
}
