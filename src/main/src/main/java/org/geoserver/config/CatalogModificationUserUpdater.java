/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

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
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.context.SecurityContextHolder;

public class CatalogModificationUserUpdater implements CatalogListener {

    static final Logger LOGGER = Logging.getLogger(CatalogModificationUserUpdater.class);

    public static final String TRACK_USER = "TRACK_USER";

    Catalog catalog;

    GeoServer geoServer;

    public CatalogModificationUserUpdater(Catalog catalog, GeoServer geoServer) {
        this.catalog = catalog;
        this.geoServer = geoServer;
        catalog.addListener(this);

        LOGGER.fine("Initiated CatalogModificationUserUpdater");
    }

    @Override
    public void handlePreAddEvent(CatalogBeforeAddEvent event) throws CatalogException {
        SettingsInfo settings = geoServer.getSettings();
        if (canTrackUser(event, settings)) {
            CatalogInfo info = event.getSource();
            info.setModifiedBy(
                    SecurityContextHolder.getContext().getAuthentication().getName());
        }
        LOGGER.finest(event + " :handlePreAddEvent");
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {}

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {}

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        SettingsInfo settings = geoServer.getSettings();
        if (canTrackUser(event, settings)) {
            CatalogInfo info = event.getSource();
            info.setModifiedBy(
                    SecurityContextHolder.getContext().getAuthentication().getName());
        }
        LOGGER.finest(event + " :handleModifyEvent");
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {}

    @Override
    public void reloaded() {}

    private static boolean canTrackUser(CatalogEvent event, SettingsInfo settings) {
        String property = GeoServerExtensions.getProperty(TRACK_USER);
        return (property == null && settings.isShowModifiedUserInAdminList() || Boolean.parseBoolean(property))
                && (event.getSource() instanceof StyleInfo
                        || event.getSource() instanceof PublishedInfo
                        || event.getSource() instanceof StoreInfo
                        || event.getSource() instanceof WorkspaceInfo);
    }
}
