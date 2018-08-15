/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import org.geoserver.catalog.Catalog;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.ReadOnlyConfiguration;
import org.geoserver.cluster.impl.handlers.DocumentFile;
import org.geoserver.cluster.impl.handlers.DocumentFileHandler;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

/** @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it */
public class JMSCatalogStylesFileHandler extends DocumentFileHandler {
    private final Catalog catalog;

    private JMSConfiguration config;
    private final GeoServerResourceLoader loader;

    public JMSCatalogStylesFileHandler(
            Catalog catalog, XStream xstream, Class clazz, GeoServerResourceLoader loader) {
        super(xstream, clazz);
        this.catalog = catalog;
        this.loader = loader;
    }

    public void setConfig(JMSConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean synchronize(DocumentFile event) throws Exception {
        if (event == null) {
            throw new NullPointerException("Incoming object is null");
        }
        if (config == null) {
            throw new IllegalStateException("Unable to load configuration");
        } else if (!ReadOnlyConfiguration.isReadOnly(config)) {
            try {
                Resource file = loader.get("styles").get(event.getResourceName());

                if (!Resources.exists(file)) {
                    final String styleAbsolutePath = event.getResourcePath();
                    if (styleAbsolutePath.indexOf("workspaces") > 0) {
                        file =
                                loader.get(
                                        styleAbsolutePath.substring(
                                                styleAbsolutePath.indexOf("workspaces")));
                    }
                }

                event.writeTo(file);
                return true;
            } catch (Exception e) {
                if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                    LOGGER.severe(
                            this.getClass()
                                    + " is unable to synchronize the incoming event: "
                                    + event);
                throw e;
            }
        }
        return true;
    }
}
