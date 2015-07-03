/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.ResourceReference;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.util.logging.Logging;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Envelope;

/**
 * A model class for the UI, hides the difference between simple layers and
 * groups, centralizes the computation of a valid preview request
 */
public class PreviewLayer {
    static final Logger LOGGER = Logging.getLogger(PreviewLayer.class);

    public enum PreviewLayerType {
        Raster, Vector, Remote, Group
    };

    LayerInfo layerInfo;

    LayerGroupInfo groupInfo;

    transient GetMapRequest request;

    public PreviewLayer(LayerInfo layerInfo) {
        this.layerInfo = layerInfo;
    }

    public PreviewLayer(LayerGroupInfo groupInfo) {
        this.groupInfo = groupInfo;
    }

    public String getName() {
        if (layerInfo != null) {
            return layerInfo.getResource().prefixedName();
        } else {
            return groupInfo.prefixedName();
        }
    }
    
    public String getWorkspace() {
        if (layerInfo != null) {
            return layerInfo.getResource().getStore().getWorkspace().getName();
        } else if (groupInfo != null && groupInfo.getWorkspace() != null){
            return groupInfo.getWorkspace().getName();
        }
        return null;
    }
    
    public ResourceReference getIcon() {
        if(layerInfo != null)
            return CatalogIconFactory.get().getSpecificLayerIcon(layerInfo);
        else
            return CatalogIconFactory.GROUP_ICON;
    }
    
    public ResourceReference getTypeSpecificIcon() {
        if(layerInfo != null)
            return CatalogIconFactory.get().getSpecificLayerIcon(layerInfo);
        else
            return CatalogIconFactory.GROUP_ICON;
    }
    
    public String getTitle() {
        if(layerInfo != null) {
            return layerInfo.getResource().getTitle();
        } else if(groupInfo != null) {
            return groupInfo.getTitle();
        } else {
            return "";
        }
    }
    
    public String getAbstract() {
        if(layerInfo != null) {
            return layerInfo.getResource().getAbstract();
        } else if(groupInfo != null) {
            return groupInfo.getAbstract();
        } else {
            return "";
        }
    }
    
    public String getKeywords() {
        if(layerInfo != null) {
            return layerInfo.getResource().getKeywords().toString();
        } else {
            return "";
        }
    }

    public PreviewLayer.PreviewLayerType getType() {
        if (layerInfo != null) {
            if (layerInfo.getType() == PublishedType.RASTER)
                return PreviewLayerType.Raster;
            else if (layerInfo.getType() == PublishedType.VECTOR)
                return PreviewLayerType.Vector;
            else
                return PreviewLayerType.Remote;
        } else {
            return PreviewLayerType.Group;
        }
    }

    /**
     * Builds a fake GetMap request
     * 
     * @param prefixedName
     * @return
     */
    GetMapRequest getRequest() {
        if (request == null) {
            GeoServerApplication app = GeoServerApplication.get();
            request = new GetMapRequest();
            Catalog catalog = app.getCatalog();
            List<MapLayerInfo> layers = expandLayers(catalog);
            request.setLayers(layers);
            request.setFormat("application/openlayers");
            
            // in the case of groups we already know about the envelope and the target SRS
            if(groupInfo != null) {
                ReferencedEnvelope bounds = groupInfo.getBounds();
                request.setBbox(bounds);
                String epsgCode = GML2EncodingUtils.epsgCode(bounds.getCoordinateReferenceSystem());
                if(epsgCode != null)
                    request.setSRS("EPSG:" + epsgCode);
            }
            try {
                DefaultWebMapService.autoSetBoundsAndSize(request);
            } catch (Exception e) {
                LOGGER.log(Level.INFO,
                        "Could not set figure out automatically a good preview link for "
                                + getName(), e);
            }
        }
        return request;
    }

    /**
     * Expands the specified name into a list of layer info names
     * 
     * @param name
     * @param catalog
     * @return
     */
    private List<MapLayerInfo> expandLayers(Catalog catalog) {
        List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();

        if (layerInfo != null) {
            layers.add(new MapLayerInfo(layerInfo));
        } else {
            for (LayerInfo l : Iterables.filter(groupInfo.getLayers(), LayerInfo.class)) {
                layers.add(new MapLayerInfo(l));
            }
        }
        return layers;
    }
    
    String getBaseUrl(String service) {
        String ws = getWorkspace();
        if(ws == null) {
            // global reference
            return ResponseUtils.buildURL("../", service, null, URLType.SERVICE);
        } else {
            return ResponseUtils.buildURL("../", ws + "/" + service, null, URLType.SERVICE);
        }
    }
    
    /**
     * Given a request and a target format, builds the WMS request
     * 
     * @param request
     * @param string
     * @return
     */
    public String getWmsLink() {
        GetMapRequest request = getRequest();
        final Envelope bbox = request.getBbox();
        if (bbox == null)
            return null;

        return getBaseUrl("wms") + "?service=WMS&version=1.1.0&request=GetMap" //
                + "&layers=" + getName() //
                + "&styles=" //
                + "&bbox=" + bbox.getMinX() + "," + bbox.getMinY() //
                + "," + bbox.getMaxX() + "," + bbox.getMaxY() //
                + "&width=" + request.getWidth() //
                + "&height=" + request.getHeight() + "&srs=" + request.getSRS();
    }
}
