/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.Serializable;
import java.util.Date;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogBeforeAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geotools.util.logging.Logging;

/** @author Imran Rajjad The listener class updates Layers,Layer Groups and Styles */
public class CatalogTimeStampUpdater implements CatalogListener {

    static final Logger LOGGER = Logging.getLogger(CatalogTimeStampUpdater.class);

    Catalog catalog;
    // GeoServer geoServer;
    public CatalogTimeStampUpdater(Catalog catalog) {

        //  this.geoServer = geoServer;
        // self registering
        this.catalog = catalog;
        catalog.addListener(this);

        LOGGER.info("Initiated CatalogTimeStampUpdater");
    }

    @Override
    public void handlePreAddEvent(CatalogBeforeAddEvent event) throws CatalogException {
        if (event.getSource() instanceof PublishedInfo) {
            Date dateCreated = new Date();
            PublishedInfo info = (PublishedInfo) event.getSource();
            info.getMetadata().putIfAbsent(PublishedInfo.TIME_CREATED, dateCreated);
        } else if (event.getSource() instanceof StyleInfo) {
            StyleInfo info = (StyleInfo) event.getSource();
            info.getMetadata().putIfAbsent(PublishedInfo.TIME_CREATED, new Date());
        } else if (event.getSource() instanceof StoreInfo) {
            StoreInfo info = (StoreInfo) event.getSource();
            info.getMetadata().put(PublishedInfo.TIME_CREATED, new Date());
        }
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        // do nothing if its not a Layer or Style
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        // do nothing if its not a Layer or Style
        if (event.getSource() instanceof PublishedInfo) {
            Date dateModified = new Date();
            PublishedInfo info = (PublishedInfo) event.getSource();
            info.getMetadata().put(PublishedInfo.TIME_MODIFIED, dateModified);
            // only for required for Layers and Layer Groups
            updateModifyEventMetadata(event, PublishedInfo.TIME_MODIFIED, dateModified);
        } else if (event.getSource() instanceof StyleInfo) {
            StyleInfo info = (StyleInfo) event.getSource();
            info.getMetadata().put(PublishedInfo.TIME_MODIFIED, new Date());
            updateModifyEventMetadata(
                    event,
                    PublishedInfo.TIME_MODIFIED,
                    info.getMetadata().get(PublishedInfo.TIME_MODIFIED, Date.class));
        } else if (event.getSource() instanceof StoreInfo) {
            StoreInfo info = (StoreInfo) event.getSource();
            info.getMetadata().put(PublishedInfo.TIME_MODIFIED, new Date());
            updateModifyEventMetadata(
                    event,
                    PublishedInfo.TIME_MODIFIED,
                    info.getMetadata().get(PublishedInfo.TIME_MODIFIED, Date.class));
        }
        LOGGER.finest(event.toString());
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {}

    @Override
    public void reloaded() {
        // TODO Auto-generated method stub

    }

    // make modification date insertion in hashmap part of event
    // so that it can be proccessed as part of layer modification properties
    private CatalogModifyEvent updateModifyEventMetadata(
            CatalogModifyEvent event, String key, Serializable value) {
        for (Object o : event.getNewValues()) {
            if (o instanceof MetadataMap) {
                MetadataMap map = (MetadataMap) o;
                map.putIfAbsent(key, value);
                return event;
            }
        }
        return event;
    }
}
