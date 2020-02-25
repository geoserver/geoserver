/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.server.events;

import java.util.List;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogModifyEvent;

/** Catalog modify event for styles that include the style file as an array of bytes. */
public class StyleModifyEvent implements CatalogModifyEvent {

    private final CatalogModifyEvent event;
    private final byte[] file;

    public StyleModifyEvent(CatalogModifyEvent event, byte[] file) {
        this.event = event;
        this.file = file;
    }

    @Override
    public CatalogInfo getSource() {
        return event.getSource();
    }

    @Override
    public List<String> getPropertyNames() {
        return event.getPropertyNames();
    }

    @Override
    public List<Object> getOldValues() {
        return event.getOldValues();
    }

    @Override
    public List<Object> getNewValues() {
        return event.getNewValues();
    }

    public byte[] getFile() {
        return file;
    }
}
