/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

/**
 * Catalog Listener that set up WMS-EO required styles.
 *
 * <p>If the styles are not present in the Catalog they will be added at initialization. If they are
 * deleted, the Listener will create them again. If they are modified, the Listener will prevent
 * changes to be saved.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public class EoStyleCatalogListener implements CatalogListener, EoStyles {

    private final Catalog catalog;
    private final GeoServerResourceLoader resourceLoader;
    private static final Logger LOGGER = Logging.getLogger(EoStyleCatalogListener.class);

    /**
     * Create WMS-EO styles if they are not present in the Catalog and start listening to Catalog
     * events.
     */
    public EoStyleCatalogListener(Catalog catalog, GeoServerResourceLoader resourceLoader)
            throws IOException {
        this.catalog = catalog;
        this.resourceLoader = resourceLoader;
        initializeStyles();
        catalog.addListener(this);
    }

    /** Create WMS-EO styles if they are not present in the Catalog */
    private void initializeStyles() throws IOException {
        for (int i = 0; i < EO_STYLE_NAMES.length; i++) {
            String name = EO_STYLE_NAMES[i];
            if (catalog.getStyleByName(name) == null) {
                initializeStyle(name, name + ".sld");
            }
        }
    }

    /**
     * Copies a WMS-EO style to the data directory and adds a catalog entry for it.
     *
     * <p>See org.geoserver.config.GeoServerLoader.initializeStyle.
     */
    private void initializeStyle(String styleName, String sld) throws IOException {
        // copy the file out to the data directory if necessary
        Resource res = resourceLoader.get(Paths.path("styles", sld));
        if (!Resources.exists(res)) {
            IOUtils.copy(EoStyleCatalogListener.class.getResourceAsStream(sld), res.out());
        }

        // create a style for it
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName(styleName);
        s.setFilename(sld);
        try {
            catalog.add(s);
        } catch (RuntimeException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    /** Recreate WMS-EO styles that have been deleted */
    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        if (event.getSource() instanceof StyleInfo) {
            // when this event has been fired the style has already been removed
            try {
                // try to find if a required style has been deleted and recreate it
                initializeStyles();
            } catch (IOException e) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }
    }

    /** Prevent changes to WMS-EO styles */
    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        /*
        if (event.getSource() instanceof StyleInfo) {
            StyleInfo style = (StyleInfo) event.getSource();
            for (String styleName : EO_STYLE_NAMES) {
                if (styleName.equals(style.getName())) {
                    throw new CatalogException("Style " + styleName + " is used by module WMS-EO and is read-only");
                }
            }
        } */
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {}

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {}

    @Override
    public void reloaded() {}
}
