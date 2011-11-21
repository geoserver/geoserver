/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.List;

import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;

/**
 * Updates the updateSequence on Catalog events.
 */
class UpdateSequenceListener implements CatalogListener, ConfigurationListener {
    
    GeoServer geoServer;
    boolean updating = false;
    
    public UpdateSequenceListener(GeoServer geoServer) {
        this.geoServer = geoServer;
        
        geoServer.getCatalog().addListener(this);
        geoServer.addListener(this);
    }
    
    synchronized void incrementSequence() {
        // prevent infinite loop on configuration update
        if(updating)
            return;
        
        try { 
            updating = true;
            GeoServerInfo gsInfo = geoServer.getGlobal();
            gsInfo.setUpdateSequence(gsInfo.getUpdateSequence() + 1);
            geoServer.save(gsInfo);
        } finally {
            updating = false;
        }
    }

    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        incrementSequence();
    }

    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        incrementSequence();
    }

    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        // never mind: we need the Post event
    }

    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        incrementSequence();
    }

    public void reloaded() {
        // never mind
    }

    public void handleGlobalChange(GeoServerInfo global, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
        // we use the post event
        
    }

    public void handleLoggingChange(LoggingInfo logging, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
        // we don't update the sequence for a logging change, the client cannot notice it   
    }

    public void handlePostGlobalChange(GeoServerInfo global) {
        incrementSequence();
    }

    public void handlePostLoggingChange(LoggingInfo logging) {
        // we don't update the sequence for a logging change, the client cannot notice it
    }

    public void handlePostServiceChange(ServiceInfo service) {
        incrementSequence();
    }

    public void handleServiceChange(ServiceInfo service, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
        // we use the post version        
    }

    @Override
    public void handleServiceRemove(ServiceInfo service) {
        incrementSequence();
    }

}