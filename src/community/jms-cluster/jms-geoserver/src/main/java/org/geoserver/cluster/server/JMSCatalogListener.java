/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.server;

import java.io.File;
import java.util.Properties;

import javax.jms.JMSException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.cluster.JMSApplicationListener;
import org.geoserver.cluster.JMSPublisher;
import org.geoserver.cluster.impl.handlers.DocumentFile;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

/**
 * JMS MASTER (Producer) Listener used to send GeoServer Catalog events over the JMS channel.
 * 
 * @see {@link JMSApplicationListener}
 * 
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 * 
 */
public class JMSCatalogListener extends JMSAbstractGeoServerProducer implements CatalogListener {

    private final static java.util.logging.Logger LOGGER = Logging.getLogger(JMSCatalogListener.class);

    private final JMSPublisher jmsPublisher;

    /**
     * Constructor
     * 
     * @param topicTemplate the getJmsTemplate() object used to send message to the topic queue
     * 
     */
    public JMSCatalogListener(final Catalog catalog, final JMSPublisher jmsPublisher) {
        super();
        this.jmsPublisher = jmsPublisher;
        catalog.addListener(this);
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
            LOGGER.fine("Incoming event of type " + event.getClass().getSimpleName()
                    + " from Catalog");
        }

        // skip incoming events if producer is not Enabled
        if (!isEnabled()) {
            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                LOGGER.fine("skipping incoming event: context is not initted");
            }
            return;
        }

        // update properties
        final Properties options = getProperties();

        try {
            // check if we may publish also the file
        	GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
            final CatalogInfo info = event.getSource();
            if (info instanceof StyleInfo) {
                final StyleInfo sInfo=((StyleInfo) info);
                WorkspaceInfo wInfo =sInfo.getWorkspace();
                Resource styleFile=null;
                
                // make sure we work fine with workspace specific styles
                if(wInfo!=null){
                    styleFile=loader.get(
                            File.separator+
                            "workspaces"+
                            File.separator+
                            wInfo.getName()+
                            File.separator+
                            "styles"+
                            File.separator+
                            sInfo.getFilename());
                    
                }else{
                    styleFile=loader.get("styles/" + sInfo.getFilename());
                }
                // checks
                if(!Resources.exists(styleFile)||!Resources.canRead(styleFile)||!(styleFile.getType() == Type.RESOURCE)){
                    throw new IllegalStateException("Unable to find style for event: "+sInfo.toString());
                }

                // transmit the file
                jmsPublisher.publish(getTopic(), getJmsTemplate(), options,
                        new DocumentFile(styleFile));
            }

            // propagate the event
            jmsPublisher.publish(getTopic(), getJmsTemplate(), options, event);
        } catch (Exception e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) {
                LOGGER.severe(e.getLocalizedMessage());
            }
            final CatalogException ex = new CatalogException(e);
            throw ex;
        }
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
            LOGGER.fine("Incoming message event of type " + event.getClass().getSimpleName()
                    + " from Catalog");
        }

        // skip incoming events until context is loaded
        if (!isEnabled()) {
            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                LOGGER.fine("skipping incoming event: context is not initted");
            }
            return;
        }

        // update properties
        Properties options = getProperties();

        try {
            jmsPublisher.publish(getTopic(), getJmsTemplate(), options, event);
        } catch (JMSException e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) {
                LOGGER.severe(e.getLocalizedMessage());
            }
            throw new CatalogException(e);
        }
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
            LOGGER.fine("Incoming message event of type " + event.getClass().getSimpleName()
                    + " from Catalog");
        }

        // skip incoming events until context is loaded
        if (!isEnabled()) {
            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                LOGGER.fine("skipping incoming event: context is not initted");
            }
            return;
        }

        // update properties
        Properties options = getProperties();

        try {
            // check if we may publish also the file
            GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
            final CatalogInfo info = event.getSource();
            if (info instanceof StyleInfo) {
                // build local datadir file style path
                Resource styleFile = loader.get("styles").get(((StyleInfo) info).getFilename());

                if (!Resources.exists(styleFile)) {
                    final String workspace = ((StyleInfo) info).getWorkspace().getName();
                    styleFile = loader.get(Paths.path("workspaces", workspace, "styles",
                            ((StyleInfo) info).getFilename()));
                }

                // publish the style xml document
                jmsPublisher.publish(getTopic(), getJmsTemplate(), options, new DocumentFile(styleFile));
            }

            // propagate the event
            jmsPublisher.publish(getTopic(), getJmsTemplate(), options, event);

        } catch (Exception e) {
            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE)) {
                LOGGER.severe(e.getLocalizedMessage());
            }
            final CatalogException ex = new CatalogException(e);
            throw ex;
        }
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
            LOGGER.fine("Incoming message event of type " + event.getClass().getSimpleName()
                    + " from Catalog");
        }

        // EAT EVENT
        // this event should be generated locally (to slaves) by the catalog
        // itself
    }

    @Override
    public void reloaded() {

        // skip incoming events until context is loaded
        if (!isEnabled()) {
            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                LOGGER.fine("skipping incoming event: context is not initted");
            }
            return;
        }

        // EAT EVENT

        // TODO disable and re-enable the producer!!!!!
        // this is potentially a problem since this listener should be the first
        // called by the GeoServer.
    }
}
