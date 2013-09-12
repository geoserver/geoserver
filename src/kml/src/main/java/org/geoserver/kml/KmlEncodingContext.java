/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.config.ServiceInfo;
import org.geoserver.kml.decorator.KmlDecoratorFactory;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.kml.sequence.CompositeList;
import org.geoserver.kml.utils.LookAtOptions;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapViewport;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.util.Converters;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;

/**
 * A class used by {@link KmlDecorator} to get the current encoding context (request, map content,
 * current layer, feature and so on).
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class KmlEncodingContext {
    
    protected boolean kmz;

    protected WMSMapContent mapContent;

    protected GetMapRequest request;

    protected List<Symbolizer> currentSymbolizers;

    protected Layer currentLayer;

    protected SimpleFeatureCollection currentFeatureCollection;

    protected SimpleFeature currentFeature;

    protected Map<String, Object> metadata = new HashMap<String, Object>();

    protected boolean descriptionEnabled;

    protected FeatureTemplate template = new FeatureTemplate();

    protected LookAtOptions lookAtOptions;

    protected WMS wms;
    
    protected Map<String, Layer> kmzGroundOverlays = new LinkedHashMap<String, Layer>();

    protected boolean placemarkForced;

    protected String superOverlayMode;

    protected boolean superOverlayEnabled;

    protected boolean networkLinksFormat;

    protected boolean extendedDataEnabled;

    protected int kmScore;

    protected ServiceInfo service;

    protected int layerIndex;
    
    protected String mode;

    public final static ReferencedEnvelope WORLD_BOUNDS_WGS84 = new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);
    protected boolean liveIcons;
    protected Map<String, Style> iconStyles;

    public KmlEncodingContext(WMSMapContent mapContent, WMS wms, boolean kmz) {
        this.mapContent = fixViewport(mapContent);
        this.request = mapContent.getRequest();
        this.wms = wms;
        this.mode = computeModeOption(request.getFormatOptions());
        this.descriptionEnabled = computeKMAttr();
        this.lookAtOptions = new LookAtOptions(request.getFormatOptions());
        this.placemarkForced = computeKmplacemark();
        this.superOverlayMode = computeSuperOverlayMode();
        this.superOverlayEnabled = computeSuperOverlayEnabled();
        this.extendedDataEnabled =  computeExtendedDataEnabled();
        this.kmScore = computeKmScore();
        this.networkLinksFormat = KMLMapOutputFormat.NL_KML_MIME_TYPE.equals(request.getFormat()) || KMZMapOutputFormat.NL_KMZ_MIME_TYPE.equals(request.getFormat());
        this.kmz = kmz;
        this.service = wms.getServiceInfo();
        this.liveIcons = true;
        this.iconStyles = new HashMap<String,Style>();
        
        Boolean autofit = Converters.convert(request.getFormatOptions().get("autofit"), Boolean.class);
        if(autofit != null && Converters.convert(autofit, Boolean.class)) {
            double width = mapContent.getMapWidth();
            double height = mapContent.getMapHeight();
            ReferencedEnvelope bbox = mapContent.getViewport().getBounds();
            double bbox_width = bbox.getWidth(); 
            double bbox_height = bbox.getHeight();
            // be on the safe side
            if(bbox_width > 0 && bbox_height > 0 & width > 0 & height > 0) {
                double ratio = bbox_width / bbox_height;
                if(bbox_width > bbox_height) {
                    height = width / ratio;
                    int h = (int) Math.ceil(height);
                    mapContent.setMapHeight(h);
                    mapContent.getRequest().setHeight(h);
                } else {
                    width = height * ratio;
                    int w = (int) Math.ceil(width);
                    mapContent.setMapWidth(w);
                    mapContent.getRequest().setWidth(w);
                }
            }
        }
    }
    
    private String computeModeOption(Map<String, String> rawKvp) {
        String mode = KvpUtils.caseInsensitiveParam(rawKvp, "mode", null);
        return mode;
    }

    /**
     * Force the output to be in WGS84
     * @param mc
     * @return
     */
    private WMSMapContent fixViewport(WMSMapContent mc) {
        MapViewport viewport = mc.getViewport();
        if(!CRS.equalsIgnoreMetadata(viewport.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84)) {
            viewport.setCoordinateReferenceSystem(DefaultGeographicCRS.WGS84);
            GetMapRequest req = mc.getRequest();
            req.setSRS("EPSG:4326");
            req.setBbox(viewport.getBounds());
        }
        
        return mc;
    }

    /**
     * Protected constructor used by WFS output format to create a fake kml encoding context
     */
    protected KmlEncodingContext() {
        
    }
    
    /**
     * Returns the the kmplacemark value (either specified in the request, or the default one)
     * 
     * @param mapContent
     * @return
     */
    boolean computeKmplacemark() {
        Object kmplacemark = request.getFormatOptions().get("kmplacemark");
        if (kmplacemark != null) {
            return Converters.convert(kmplacemark, Boolean.class);
        } else {
            return wms.getKmlPlacemark();
        }
    }

    
    /**
     * Returns the the kmattr value (either specified in the request, or the default one)
     * 
     * @param mapContent
     * @return
     */
    boolean computeKMAttr() {
        Object kmattr = request.getFormatOptions().get("kmattr");
        if (kmattr == null) {
            kmattr = request.getRawKvp().get("kmattr");
        }
        if (kmattr != null) {
            return Converters.convert(kmattr, Boolean.class);
        } else {
            return wms.getKmlKmAttr();
        }
    }
    
    private int computeKmScore() {
        int kmScore = wms.getKmScore();
        Map fo = request.getFormatOptions();
        Object kmScoreObj = fo.get("kmscore");
        if (kmScoreObj != null) {
            kmScore = (Integer) kmScoreObj;
        }

        return kmScore;
    }

    /**
     * Checks if the extended data is enabled or not
     * @param request
     * @return
     */
    boolean computeExtendedDataEnabled() {
        Map formatOptions = request.getFormatOptions();
        Boolean extendedData = Converters.convert(formatOptions.get("extendedData"), Boolean.class); 
        if (extendedData == null) {
            extendedData = Boolean.FALSE;
        }

        return extendedData;
    }
    
    /**
     * Checks if the superoverlay is enabled or not
     * @param request
     * @return
     */
    boolean computeSuperOverlayEnabled() {
        Map formatOptions = request.getFormatOptions();
        Boolean superoverlay = (Boolean) formatOptions.get("superoverlay");
        if (superoverlay == null) {
            superoverlay = Boolean.FALSE;
        }

        return superoverlay;
    }
    
    
    /**
     * Returns the superoverlay mode (either specified in the request, or the default one)
     * @return
     */
    String computeSuperOverlayMode() {
        String overlayMode = (String) request.getFormatOptions().get("superoverlay_mode");
        if(overlayMode != null) {
            return overlayMode;
        } 
        
        overlayMode = (String) request.getFormatOptions().get("overlayMode");
        if(overlayMode != null) {
            return overlayMode;
        } else {
            return wms.getKmlSuperoverlayMode();
        }
    }
   
    /**
     * Returns the {@link KmlDecorator} objects for the specified Feature class
     * 
     * @return
     */
    public List<KmlDecorator> getDecoratorsForClass(Class<? extends Feature> clazz) {
        List<KmlDecoratorFactory> factories = GeoServerExtensions
                .extensions(KmlDecoratorFactory.class);
        List<KmlDecorator> result = new ArrayList<KmlDecorator>();
        for (KmlDecoratorFactory factory : factories) {
            KmlDecorator decorator = factory.getDecorator(clazz, this);
            if (decorator != null) {
                result.add(decorator);
            }
        }

        return result;
    }
    
    /**
     * Adds features to the folder own list
     * 
     * @param folder
     * @param features
     */
    public void addFeatures(Folder folder, List<Feature> features) {
        List<Feature> originalFeatures = folder.getFeature();
        if (originalFeatures == null || originalFeatures.size() == 0) {
            folder.setFeature(features);
        } else {
            // in this case, compose the already existing features with the
            // dynamically generated ones
            folder.setFeature(new CompositeList<Feature>(originalFeatures, features));
        }
    }
    
    /**
     * Adds features to the document own list
     * 
     * @param folder
     * @param features
     */
    public void addFeatures(Document document, List<Feature> features) {
        List<Feature> originalFeatures = document.getFeature();
        if (originalFeatures == null || originalFeatures.size() == 0) {
            document.setFeature(features);
        } else {
            // in this case, compose the already existing features with the
            // dynamically generated ones
            document.setFeature(new CompositeList<Feature>(originalFeatures, features));
        }
    }

    public WMSMapContent getMapContent() {
        return mapContent;
    }

    public void setMapContent(WMSMapContent mapContent) {
        this.mapContent = mapContent;
    }

    public GetMapRequest getRequest() {
        return request;
    }

    public void setRequest(GetMapRequest request) {
        this.request = request;
    }

    public List<Symbolizer> getCurrentSymbolizers() {
        return currentSymbolizers;
    }

    public void setCurrentSymbolizers(List<Symbolizer> symbolizers) {
        this.currentSymbolizers = symbolizers;
    }

    public Layer getCurrentLayer() {
        return currentLayer;
    }

    public void setCurrentLayer(Layer currentLayer) {
        this.currentLayer = currentLayer;
        this.layerIndex++;
    }

    public SimpleFeature getCurrentFeature() {
        return currentFeature;
    }

    public void setCurrentFeature(SimpleFeature currentFeature) {
        this.currentFeature = currentFeature;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public boolean isDescriptionEnabled() {
        return descriptionEnabled;
    }

    public void setDescriptionEnabled(boolean descriptionEnabled) {
        this.descriptionEnabled = descriptionEnabled;
    }

    public FeatureTemplate getTemplate() {
        return template;
    }

    public LookAtOptions getLookAtOptions() {
        return lookAtOptions;
    }

    public WMS getWms() {
        return wms;
    }

    public SimpleFeatureCollection getCurrentFeatureCollection() {
        return currentFeatureCollection;
    }

    public void setCurrentFeatureCollection(SimpleFeatureCollection currentFeatureCollection) {
        this.currentFeatureCollection = currentFeatureCollection;
    }

    public boolean isKmz() {
        return kmz;
    }
    
    /**
     * Adds a layer to be generated as ground overlay in the kmz package
     * @param imagePath The path of the ground overlay image inside the kmz archive
     * @param layer
     */
    public void addKmzGroundOverlay(String imagePath, Layer layer) {
        if(!kmz) {
            throw new IllegalStateException("Cannot add ground " +
            		"overlay layers, the output is not supposed to be a KMZ");
        }
        this.kmzGroundOverlays.put(imagePath, layer);
    }

    /**
     * Returns the list of ground overlay layers to be included in the KMZ response
     * @return
     */
    public Map<String, Layer> getKmzGroundOverlays() {
        return kmzGroundOverlays;
    }

    public boolean isPlacemarkForced() {
        return placemarkForced;
    }

    public void setPlacemarkForced(boolean placemarkForced) {
        this.placemarkForced = placemarkForced;
    }

    public boolean isSuperOverlayEnabled() {
        return superOverlayEnabled;
    }

    public String getSuperOverlayMode() {
        return superOverlayMode;
    }

    public boolean isNetworkLinksFormat() {
        return networkLinksFormat;
    }

    public boolean isExtendedDataEnabled() {
        return extendedDataEnabled;
    }

    public int getKmScore() {
        return kmScore;
    }

    public ServiceInfo getService() {
        return service;
    }

    /**
     * Returns a list of the feature types to be encoded. Will provide a feature type only for the
     * vector layers, a null will be placed where a layer of different nature is found
     * @return
     */
    public List<SimpleFeatureType> getFeatureTypes() {
        List<SimpleFeatureType> results = new ArrayList<SimpleFeatureType>();
        for(Layer layer : mapContent.layers()) {
            if(layer instanceof FeatureLayer) {
                results.add((SimpleFeatureType) layer.getFeatureSource().getSchema());
            } else {
                results.add(null);
            }
        }
        
        return results;
    }

    /**
     * Returns the current feature type is the current layer is made of vector features, null otherwise
     * @return
     */
    public SimpleFeatureType getCurrentFeatureType() {
        if(currentLayer instanceof FeatureLayer) {
            FeatureLayer fl = (FeatureLayer) currentLayer;
            return (SimpleFeatureType) fl.getFeatureSource().getSchema();
        }
        return null;
    }

    /**
     * Returns the current layer index in the request
     * @return
     */
    public int getCurrentLayerIndex() {
        return layerIndex;
    }

    public boolean isLiveIcons() {
        return liveIcons;
    }

    public void setLiveIcons(boolean liveIcons) {
        this.liveIcons = liveIcons;
    }

    public Map<String, Style> getIconStyles() {
        return iconStyles;
    }

    public String getMode() {
        return mode;
    }

}
