/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.DefaultGeoServerLoader;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerResourcePersister;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.JDBCConfigProperties;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

public class JDBCGeoServerLoader extends DefaultGeoServerLoader {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logging.getLogger(JDBCGeoServerLoader.class);

    private CatalogFacade catalogFacade;

    private GeoServerFacade geoServerFacade;

    private JDBCConfigProperties config;

    private int importSteps = 2;

    public JDBCGeoServerLoader(GeoServerResourceLoader resourceLoader, JDBCConfigProperties config)
            throws Exception {
        super(resourceLoader);
        this.config = config;
    }

    public void setCatalogFacade(CatalogFacade catalogFacade) throws IOException {
        this.catalogFacade = catalogFacade;

        if (!config.isEnabled()) {
            return;
        }

        ConfigDatabase configDatabase = ((JDBCCatalogFacade) catalogFacade).getConfigDatabase();

        Resource initScript = config.isInitDb() ? config.getInitScript() : null;
        configDatabase.initDb(initScript);

        config.setInitDb(false);
        config.save();
    }

    public void setGeoServerFacade(GeoServerFacade geoServerFacade) {
        this.geoServerFacade = geoServerFacade;
    }

    @Override
    protected void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        if (!config.isEnabled()) {
            super.loadCatalog(catalog, xp);
            return;
        }

        Stopwatch sw = Stopwatch.createStarted();
        loadCatalogInternal(catalog, xp);
        sw.stop();

        catalog.addListener(new GeoServerResourcePersister(catalog));
        // System.err.println("Loaded catalog in " + sw.toString());
    }

    private void loadCatalogInternal(Catalog catalog, XStreamPersister xp) throws Exception {
        ((CatalogImpl) catalog).setFacade(catalogFacade);

        // if this is the first time loading up with jdbc configuration, migrate from old
        // file based structure
        if (config.isImport()) {
            readCatalog(catalog, xp);
            decImportStep();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void loadGeoServer(GeoServer geoServer, XStreamPersister xp) throws Exception {
        if (!config.isEnabled()) {
            super.loadGeoServer(geoServer, xp);
            return;
        }

        ((GeoServerImpl) geoServer).setFacade(geoServerFacade);

        // if this is the first time loading up with bdb je configuration, migrate from old
        // file based structure
        if (config.isImport()) {
            readConfiguration(geoServer, xp);
            decImportStep();
        }

        // do a post check to ensure things were loaded, for instance if we are starting from
        // an empty data directory all objects will be empty
        // TODO: this should really be moved elsewhere
        if (geoServer.getGlobal() == null) {
            geoServer.setGlobal(geoServer.getFactory().createGlobal());
        }
        if (geoServer.getLogging() == null) {
            geoServer.setLogging(geoServer.getFactory().createLogging());
        }

        // also ensure we have a service configuration for every service we know about
        final List<XStreamServiceLoader> loaders =
                GeoServerExtensions.extensions(XStreamServiceLoader.class);
        for (XStreamServiceLoader l : loaders) {
            ServiceInfo s = geoServer.getService(l.getServiceClass());
            if (s == null) {
                geoServer.add(l.create(geoServer));
            }
        }
    }

    private void decImportStep() throws IOException {
        if (--importSteps == 0) {

            // import completed, reset flag
            config.setImport(false);
            config.save();
        }
    }

    @Override
    public void reload() throws Exception {
        super.reload();
    }
}
