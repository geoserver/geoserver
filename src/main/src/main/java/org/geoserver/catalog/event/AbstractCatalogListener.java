/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event;

import org.geoserver.catalog.CatalogException;

/**
 * A base class for {@link CatalogListener} that implements all listener methods without any action. Useful for
 * listeners that are only interested in a subset of events.
 */
public class AbstractCatalogListener implements CatalogListener {

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        // nothing to do
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        // nothing to do
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        // nothing to do
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        // nothing to do
    }

    @Override
    public void reloaded() {
        // nothing to do
    }
}
