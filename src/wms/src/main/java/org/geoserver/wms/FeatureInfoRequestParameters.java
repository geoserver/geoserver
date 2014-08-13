/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geoserver.platform.ServiceException;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.styling.Style;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * Wraps the large number of information normally extracted from a feature info request into a
 * single object, provides facilities to access extra information about the current layer (view
 * parameters, styles)
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class FeatureInfoRequestParameters {

    int x;

    int y;

    int buffer;

    List<Map<String, String>> viewParams;

    GetMapRequest getMapReq;

    CoordinateReferenceSystem requestedCRS;

    int width;

    int height;

    ReferencedEnvelope bbox;

    double scaleDenominator;
    
    List<Filter> filters;
    
    List<MapLayerInfo> layers;
    
    List<Style> styles;

    List<Object> elevations;

    List<Object> times;

    FilterFactory2 ff;

    private List<List<String>> propertyNames;

    int currentLayer = 0;

    int maxFeatures;

    public FeatureInfoRequestParameters(GetFeatureInfoRequest request) {
        // use the layer of the QUERY_LAYERS parameter, not the LAYERS one
        this.layers = request.getQueryLayers();
        
        this.filters = request.getGetMapRequest().getFilter();
        this.styles = getStyles(request, layers);
        this.x = request.getXPixel();
        this.y = request.getYPixel();
        this.buffer = request.getGetMapRequest().getBuffer();
        this.viewParams = request.getGetMapRequest().getViewParams();
        this.getMapReq = request.getGetMapRequest();
        this.requestedCRS = getMapReq.getCrs(); // optional, may be null
        this.maxFeatures = request.getFeatureCount();

        // basic information about the request
        this.width = getMapReq.getWidth();
        this.height = getMapReq.getHeight();
        this.bbox = new ReferencedEnvelope(getMapReq.getBbox(), getMapReq.getCrs());
        this.scaleDenominator = RendererUtilities.calculateOGCScale(bbox, width, null);
        this.elevations = request.getGetMapRequest().getElevation();
        this.times = request.getGetMapRequest().getTime();
        this.ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

        this.propertyNames = request.getPropertyNames();
    }
    
    /**
     * Grab the list of styles for each query layer, we'll use them to auto-evaluate the
     * GetFeatureInfo radius if the user did not specify one
     * 
     * @param request
     * @param layers
     * @return
     */
    private List<Style> getStyles(final GetFeatureInfoRequest request, List<MapLayerInfo> layers) {
        List<Style> getMapStyles = request.getGetMapRequest().getStyles();
        List<Style> styles = new ArrayList<Style>();
        List<MapLayerInfo> getMapLayers = request.getGetMapRequest().getLayers();
        for (int i = 0; i < layers.size(); i++) {
            final String targetLayer = layers.get(i).getName();
            Style s = null;
            for (int j = 0; j < getMapLayers.size(); j++) {
                if (getMapLayers.get(j).getName().equals(targetLayer)) {
                    if (getMapStyles != null && getMapStyles.size() > j) {
                        s = getMapStyles.get(j);
                    } else {
                        s = getMapLayers.get(j).getDefaultStyle();
                    }
                    break;
                }
            }
            if(s != null) {
                styles.add(s);
            } else {
                throw new ServiceException("Failed to locate style for layer " + targetLayer);
            }
        }
        return styles;
    }

    /**
     * Moves to the next requested layer
     */
    void nextLayer() {
        currentLayer++;
    }
    
    
    /**
     * Returns the current layer
     * 
     * @return
     */
    public MapLayerInfo getLayer() {
        return layers.get(currentLayer);
    }
    
    public Style getStyle() {
        return styles.get(currentLayer);
    }

    public Filter getFilter() {
        if(filters == null || filters.size() <= currentLayer) {
            return null;
        } else {
            return filters.get(currentLayer);
        }
    }

    

    /**
     * The property names for the specified layer (if any, null otherwise)
     * 
     * @return
     */
    public String[] getPropertyNames() {
        if (propertyNames == null || propertyNames.size() == 0
                || propertyNames.get(currentLayer) == null) {
            return Query.ALL_NAMES;
        } else {
            List<String> layerPropNames = propertyNames.get(currentLayer);
            return (String[]) layerPropNames.toArray(new String[layerPropNames.size()]);
        }
    }

    /**
     * The view parameters for the current layer
     * 
     * @return
     */
    public Map<String, String> getViewParams() {
        if (viewParams == null || viewParams.size() < currentLayer) {
            return null;
        } else {
            return viewParams.get(currentLayer);
        }
    }

    /**
     * The x of the clicked pixel on the raster output map
     */
    public int getX() {
        return x;
    }

    /**
     * The y of the clicked pixel on the raster output map
     */
    public int getY() {
        return y;
    }

    /**
     * The buffer, as specified by the user
     */
    public int getBuffer() {
        return buffer;
    }

    /**
     * The requested coordinate reference system
     * 
     * @return
     */
    public CoordinateReferenceSystem getRequestedCRS() {
        return requestedCRS;
    }

    /**
     * Pixel width of the requested map
     */
    public int getWidth() {
        return width;
    }

    /**
     * Pixel width of the requested map
     */
    public int getHeight() {
        return height;
    }

    /**
     * Real world bounds of the requested map
     */
    public ReferencedEnvelope getRequestedBounds() {
        return bbox;
    }

    /**
     * The scale denominator of the requested map
     */
    public double getScaleDenominator() {
        return scaleDenominator;
    }

    /**
     * The elevations in the request (if any)
     */
    public List<Object> getElevations() {
        return elevations;
    }

    /**
     * The elevations in the request (if any)
     */
    public List<Object> getTimes() {
        return times;
    }

    /**
     * A filter factory suitable to build filters
     * 
     * @return
     */
    public FilterFactory2 getFilterFactory() {
        return ff;
    }

    /**
     * The GetMap request wrapped by the GetFeatureInfo one
     * 
     * @return
     */
    public GetMapRequest getGetMapRequest() {
        return getMapReq;
    }


}
