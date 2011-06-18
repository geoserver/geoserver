/* Copyright (c) 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.ows.Dispatcher;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.config.Configuration;
import org.geowebcache.config.meta.ServiceInformation;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSGeoServerHelper;
import org.geowebcache.layer.wms.WMSLayer;

public class CatalogConfiguration implements Configuration {
    private static Logger log = Logging.getLogger("org.geoserver.gwc.GWCCatalogListener");

    private final Catalog cat;

    private final GridSetBroker gridSetBroker;

    private final Dispatcher gsDispatcher;

    private final Map<String, TileLayer> layers;

    private final int[] metaFactors = { 4, 4 };

    private final String wmsUrl = null;

    private final List<String> mimeFormats;

    /**
     * 
     * @param cat
     * @param gridSetBroker
     * @param gsDispatcher
     */
    public CatalogConfiguration(final GridSetBroker gridSetBroker, final Catalog cat,
            final Dispatcher gsDispatcher) {

        this.cat = cat;
        this.gridSetBroker = gridSetBroker;
        this.gsDispatcher = gsDispatcher;

        layers = new HashMap<String, TileLayer>();

        mimeFormats = new ArrayList<String>(5);
        mimeFormats.add("image/png");
        mimeFormats.add("image/gif");
        mimeFormats.add("image/png8");
        mimeFormats.add("image/jpeg");
        mimeFormats.add("application/vnd.google-earth.kml+xml");
    }

    /**
     * 
     * @see org.geowebcache.config.Configuration#getIdentifier()
     */
    public String getIdentifier() throws GeoWebCacheException {
        return "GeoServer Catalog Configuration";
    }

    public Catalog getCatalog() {
        return cat;
    }

    /**
     * 
     * @see org.geowebcache.config.Configuration#getServiceInformation()
     */
    public ServiceInformation getServiceInformation() throws GeoWebCacheException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.geowebcache.config.Configuration#isRuntimeStatsEnabled()
     */
    public boolean isRuntimeStatsEnabled() {
        return true;
    }

    /**
     * 
     * @see org.geowebcache.config.Configuration#getTileLayers(boolean)
     */
    public synchronized List<TileLayer> getTileLayers(boolean reload) throws GeoWebCacheException {
        if (reload) {
            layers.clear();
        }
        if (layers.isEmpty()) {
            // Adding normal layers
            for (LayerInfo li : cat.getLayers()) {
                createLayer(li);
            }

            // Adding layer groups
            for (LayerGroupInfo lgi : cat.getLayerGroups()) {
                createLayer(lgi);
            }
        }
        log.fine("Responding with " + layers.size()
                + " to getTileLayers() request from TileLayerDispatcher");

        return new ArrayList<TileLayer>(layers.values());
    }

    /**
     * 
     * @param li
     * @return
     */
    public TileLayer createLayer(LayerInfo li) {
        final ResourceInfo resourceInfno = li.getResource();
        final String layerName = resourceInfno.getPrefixedName();
        final String[] wmsURL = getWMSUrl();
        final String wmsStyles = null;
        final String wmsLayers = resourceInfno.getPrefixedName();
        ReferencedEnvelope latLonBounds = resourceInfno.getLatLonBoundingBox();
        if (latLonBounds == null) {
            log.severe("LatLonBoundingBox of " + li.getName()
                    + " is null, can't create a tile layer for it");
            return null;
        }
        final Hashtable<String, GridSubset> subSets = getGrids(latLonBounds);
        final List<ParameterFilter> parameterFilters = null;
        final String vendorParams = null;
        final boolean queryable = true;

        WMSLayer retLayer = new WMSLayer(layerName, wmsURL, wmsStyles, wmsLayers, mimeFormats,
                subSets, parameterFilters, metaFactors, vendorParams, queryable);

        retLayer.setBackendTimeout(120);
        retLayer.setSourceHelper(new WMSGeoServerHelper(this.gsDispatcher));

        retLayer.initialize(gridSetBroker);

        layers.put(layerName, retLayer);
        return retLayer;
    }

    public TileLayer createLayer(LayerGroupInfo lgi) {
        ReferencedEnvelope latLonBounds = null;
        try {
            latLonBounds = lgi.getBounds().transform(CRS.decode("EPSG:4326"), true);
        } catch (Exception e) {
            log.warning(e.getMessage());
        }

        if (latLonBounds == null) {
            log.severe("GWCCatalogListener had problems getting or reprojecting " + lgi.getBounds()
                    + " to EPSG:4326");

            return null;
        }

        final String layerName = lgi.getName();
        final String[] wmsURL = getWMSUrl();
        final String wmsStyles = null;
        final String wmsLayers = lgi.getName();
        final Hashtable<String, GridSubset> subSets = getGrids(latLonBounds);
        final List<ParameterFilter> parameterFilters = null;
        final String vendorParams = null;
        final boolean queryable = true;

        WMSLayer retLayer = new WMSLayer(layerName, wmsURL, wmsStyles, wmsLayers, mimeFormats,
                subSets, parameterFilters, metaFactors, vendorParams, queryable);

        retLayer.setBackendTimeout(120);
        retLayer.setSourceHelper(new WMSGeoServerHelper(this.gsDispatcher));
        retLayer.initialize(gridSetBroker);

        layers.put(layerName, retLayer);
        return retLayer;
    }

    private String[] getWMSUrl() {
        String[] strs = { wmsUrl };
        return strs;
    }

    private Hashtable<String, GridSubset> getGrids(ReferencedEnvelope env) {
        double minX = env.getMinX();
        double minY = env.getMinY();
        double maxX = env.getMaxX();
        double maxY = env.getMaxY();

        BoundingBox bounds4326 = new BoundingBox(minX, minY, maxX, maxY);

        BoundingBox bounds900913 = new BoundingBox(longToSphericalMercatorX(minX),
                latToSphericalMercatorY(minY), longToSphericalMercatorX(maxX),
                latToSphericalMercatorY(maxY));

        Hashtable<String, GridSubset> grids = new Hashtable<String, GridSubset>(2);

        GridSubset gridSubset4326 = GridSubsetFactory.createGridSubSet(
                gridSetBroker.WORLD_EPSG4326, bounds4326, 0, 25);

        grids.put(gridSetBroker.WORLD_EPSG4326.getName(), gridSubset4326);

        GridSubset gridSubset900913 = GridSubsetFactory.createGridSubSet(
                gridSetBroker.WORLD_EPSG3857, bounds900913, 0, 25);

        grids.put(gridSetBroker.WORLD_EPSG3857.getName(), gridSubset900913);

        return grids;
    }

    private double longToSphericalMercatorX(double x) {
        return (x / 180.0) * 20037508.34;
    }

    private double latToSphericalMercatorY(double y) {
        if (y > 85.05112) {
            y = 85.05112;
        }

        if (y < -85.05112) {
            y = -85.05112;
        }

        y = (Math.PI / 180.0) * y;
        double tmp = Math.PI / 4.0 + y / 2.0;
        return 20037508.34 * Math.log(Math.tan(tmp)) / Math.PI;
    }

    public synchronized void removeLayer(String layerName) {
        layers.remove(layerName);
    }

}
