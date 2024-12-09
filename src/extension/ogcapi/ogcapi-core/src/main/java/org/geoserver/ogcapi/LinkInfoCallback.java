/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.Request;
import org.geoserver.platform.Service;
import org.springframework.stereotype.Component;

/**
 * Adds links to the OGC API documents, based on the {@link LinkInfo} objects attached to either the
 * {@link ResourceInfo}, the {@link PublishedInfo} object, or the {@link SettingsInfo} object.
 * Filters out links that are not applicable to the current service.
 */
@Component
public class LinkInfoCallback implements DocumentCallback {

    GeoServer geoServer;

    public LinkInfoCallback(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public void apply(Request dr, AbstractDocument document) {
        if (document instanceof AbstractCollectionDocument) {
            decorateCollection(dr, (AbstractCollectionDocument) document);
        } else if (isCollectionsDocument(dr)) {
            decorateCollections(dr, document);
        }
    }

    private void decorateCollections(Request dr, AbstractDocument document) {
        Class<?> serviceClass = getServiceClass(dr);
        SettingsInfo settings = geoServer.getSettings();
        LinkInfoConverter.addLinksToDocument(document, settings, serviceClass);
    }

    private boolean isCollectionsDocument(Request dr) {
        return dr.getPath().equals("collections");
    }

    private void decorateCollection(Request dr, AbstractCollectionDocument document) {
        Object subject = document.getSubject();
        Class<?> serviceClass = getServiceClass(dr);
        if (subject instanceof ResourceInfo) {
            ResourceInfo resource = (ResourceInfo) subject;
            LinkInfoConverter.addLinksToDocument(document, resource, serviceClass);
        } else if (subject instanceof PublishedInfo) {
            PublishedInfo layer = (PublishedInfo) subject;
            LinkInfoConverter.addLinksToDocument(document, layer, serviceClass);
        }
    }

    private static Class<?> getServiceClass(Request dr) {
        Service service = dr.getOperation().getService();
        return service != null ? service.getService().getClass() : null;
    }
}
