/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.opengis.util.ProgressListener;

@SuppressWarnings("serial")
public class WMTSLayerInfoImpl extends ResourceInfoImpl implements WMTSLayerInfo {

    protected WMTSLayerInfoImpl() {}

    public WMTSLayerInfoImpl(Catalog catalog) {
        super(catalog);
    }

    public WMTSLayer getWMTSLayer(ProgressListener listener) throws IOException {
        return catalog.getResourcePool().getWMTSLayer(this);
    }

    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public WMTSStoreInfo getStore() {
        return (WMTSStoreInfo) super.getStore();
    }
}
