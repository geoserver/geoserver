/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geotools.ows.wmts.model.WMTSLayer;
import org.opengis.util.ProgressListener;

/**
 * Wraps a {@link WMSLayerInfo} so that it will return secured layers and WMS stores
 *
 * @author Emanuele Tajariol (etj at geo-solutions dot it)
 */
public class SecuredWMTSLayerInfo extends DecoratingWMTSLayerInfo {

    WrapperPolicy policy;

    public SecuredWMTSLayerInfo(WMTSLayerInfo delegate, WrapperPolicy policy) {
        super(delegate);
        this.policy = policy;
    }

    @Override
    public WMTSLayer getWMTSLayer(ProgressListener listener) throws IOException {
        WMTSLayer layer = super.getWMTSLayer(listener);
        if (layer == null) {
            return layer;
        } else {
            return new SecuredWMTSLayer(layer, policy);
        }
    }

    @Override
    public WMTSStoreInfo getStore() {
        return new SecuredWMTSStoreInfo(delegate.getStore(), policy);
    }

    @Override
    public void setStore(StoreInfo store) {
        // need to make sure the store isn't secured
        super.setStore((StoreInfo) SecureCatalogImpl.unwrap(store));
    }
}
