/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.IOException;
import java.util.List;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.Catalog;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.ReadOnlyConfiguration;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A Read Only persister which inhibits write operation on the disk (if enabled)<br>
 * Note: to work this class is declared in the same package of the extending one since some member
 * are declared as package protected.
 *
 * @author carlo cancellieri - GeoSolutions SAS
 */
public class ReadOnlyGeoServerLoader extends DefaultGeoServerLoader {

    private boolean enabled = false;

    @Autowired public JMSConfiguration config;

    public ReadOnlyGeoServerLoader(final GeoServerResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    @PostConstruct
    private void init() {
        enabled = ReadOnlyConfiguration.isReadOnly(config);
    }

    protected synchronized void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        if (enabled) {
            catalog.setResourceLoader(resourceLoader);
            readCatalog(catalog, xp);
        } else {
            super.loadCatalog(catalog, xp);
        }
    }

    protected synchronized void loadGeoServer(final GeoServer geoServer, XStreamPersister xp)
            throws Exception {
        if (enabled) {
            readConfiguration(geoServer, xp);
        } else {
            super.loadGeoServer(geoServer, xp);
        }
    }

    @Override
    protected void initializeStyles(Catalog catalog, XStreamPersister xp) throws IOException {
        super.initializeStyles(catalog, xp);
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized void enable(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            // remove Default persister
            if (configPersister != null) {
                geoserver.removeListener(configPersister);
                configPersister = null;
            }
            // remove Default listener
            if (listener != null) {
                geoserver.removeListener(listener);
                listener = null;
            }
        } else {
            if (listener == null) {
                // add event listener which persists changes
                final List<XStreamServiceLoader> loaders =
                        GeoServerExtensions.extensions(XStreamServiceLoader.class);
                listener = new ServicePersister(loaders, geoserver);
                geoserver.addListener(listener);
            }
            if (configPersister == null) {
                configPersister =
                        new GeoServerConfigPersister(resourceLoader, xpf.createXMLPersister());
                // attach back the persister
                geoserver.addListener(configPersister);
            }
        }
    }
}
