/*
 *  (c) 2018 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */
package org.geoserver.wfs3.response;

import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.wfs3.NCNameResourceCodec;

public class CollectionHTMLResponse extends AbstractHTMLResponse {

    public CollectionHTMLResponse(GeoServerResourceLoader loader, GeoServer geoServer) {
        super(CollectionDocument.class, loader, geoServer);
    }

    @Override
    protected String getTemplateName(Object value) {
        return "collection.ftl";
    }

    @Override
    protected ResourceInfo getResource(Object value) {
        CollectionDocument cd = (CollectionDocument) value;
        List<LayerInfo> layers =
                NCNameResourceCodec.getLayers(geoServer.getCatalog(), cd.getName());
        if (!layers.isEmpty()) {
            return layers.get(0).getResource();
        }
        return null;
    }

    @Override
    protected String getFileName(Object value, Operation operation) {
        return ((CollectionDocument) value).getName();
    }
}
