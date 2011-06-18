/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import static org.geoserver.gwc.web.CachedLayerInfo.TYPE.*;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.web.CachedLayerInfo.TYPE;
import org.geotools.util.logging.Logging;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;

/**
 * A loadable model for {@link TileLayer}s used by {@link CachedLayerProvider}.
 * <p>
 * Warning, don't use it in a tabbed form or in any other places where you might need to keep the
 * modifications in a resource stable across page loads.
 * </p>
 */
@SuppressWarnings("serial")
public class CachedLayerDetachableModel extends LoadableDetachableModel<CachedLayerInfo> {

    private static final Logger LOGGER = Logging.getLogger(CachedLayerDetachableModel.class);

    private String name;

    public CachedLayerDetachableModel(CachedLayerInfo layer) {
        super(layer);
        this.name = layer.getName();
    }

    @Override
    protected CachedLayerInfo load() {
        GWC facade = GWC.get();
        return CachedLayerDetachableModel.create(name, facade);
    }

    static CachedLayerInfo create(final String name, final GWC gwc) {
        final TileLayer layer = gwc.getTileLayerByName(name);
        CachedLayerInfo info = new CachedLayerInfo();
        info.setName(name);
        info.setType(getType(layer));
        boolean enabled = layer.isEnabled();
        info.setEnabled(enabled);
        if (gwc.isDiskQuotaAvailable()) {
            info.setQuotaLimit(gwc.getQuotaLimit(name));
            info.setQuotaUsed(gwc.getUsedQuota(name));
        }
        if(!enabled && (layer instanceof GeoServerTileLayer)){
            String error = ((GeoServerTileLayer)layer).getConfigErrorMessage();
            info.setConfigErrorMessage(error);
        }
        return info;
    }

    private static TYPE getType(final TileLayer layer) {
        if (layer instanceof WMSLayer) {
            return WMS;
        } else if (layer instanceof GeoServerTileLayer) {
            GeoServerTileLayer gtl = (GeoServerTileLayer) layer;
            LayerInfo li;
            if (null != (li = gtl.getLayerInfo())) {
                ResourceInfo resource = li.getResource();
                if (resource instanceof FeatureTypeInfo) {
                    return VECTOR;
                } else if (resource instanceof CoverageInfo) {
                    return RASTER;
                } else if (resource instanceof WMSLayerInfo) {
                    return WMS;
                }
            } else if (null != gtl.getLayerGroupInfo()) {
                return LAYERGROUP;
            }
        }
        LOGGER.info("Unknown TileLayer type, returning OTHER: " + layer.getClass().getName());
        return OTHER;
    }

    public static List<CachedLayerInfo> getItems(GWC gwc) {
        List<CachedLayerInfo> lazyList = new LazyCachedLayerInfoList(gwc);
        return lazyList;
    }

    private static class LazyCachedLayerInfoList extends AbstractList<CachedLayerInfo> {

        private final GWC gwc;

        private final List<String> layerNames;

        public LazyCachedLayerInfoList(final GWC gwc) {
            this.gwc = gwc;
            List<String> names = new ArrayList<String>(gwc.getTileLayerNames());
            Collections.sort(names);
            this.layerNames = names;
        }

        @Override
        public CachedLayerInfo get(final int index) {
            final String name = layerNames.get(index);
            return CachedLayerDetachableModel.create(name, gwc);
        }

        @Override
        public int size() {
            return layerNames.size();
        }
    }

}
