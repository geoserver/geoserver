/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.Date;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogBeforeAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geotools.util.logging.Logging;

/**
 * @author Imran Rajjad
 *     <p>This listener maintains creation/modfication timestamps for following catalog elements
 *     Layers Layer Groups Styles, Stores, Workspacses
 */
public class CatalogTimeStampUpdater implements CatalogListener {

    static final Logger LOGGER = Logging.getLogger(CatalogTimeStampUpdater.class);

    Catalog catalog;

    public CatalogTimeStampUpdater(Catalog catalog) {

        // self registering
        this.catalog = catalog;
        catalog.addListener(this);

        LOGGER.info("Initiated CatalogTimeStampUpdater");
    }

    @Override
    public void handlePreAddEvent(CatalogBeforeAddEvent event) throws CatalogException {

        if (event.getSource() instanceof PublishedInfo
                || event.getSource() instanceof StyleInfo
                || event.getSource() instanceof PublishedInfo
                || event.getSource() instanceof StoreInfo
                || event.getSource() instanceof WorkspaceInfo) {
            CatalogInfo info = (CatalogInfo) event.getSource();
            info.setDateCreated(new Date());
        }
        LOGGER.finest(event.toString() + " :handlePreAddEvent");
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {}

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {}

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {

        if (event.getSource() instanceof PublishedInfo
                || event.getSource() instanceof StyleInfo
                || event.getSource() instanceof PublishedInfo
                || event.getSource() instanceof StoreInfo
                || event.getSource() instanceof WorkspaceInfo) {

            CatalogInfo info = (CatalogInfo) event.getSource();
            Date dateModified = new Date();
            info.setDateModified(dateModified);
        }
        LOGGER.finest(event.toString() + " :handleModifyEvent");
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {}

    @Override
    public void reloaded() {}
}
