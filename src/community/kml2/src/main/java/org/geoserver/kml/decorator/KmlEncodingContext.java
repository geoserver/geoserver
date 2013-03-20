/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.kml.KMLUtils;
import org.geoserver.kml.LookAtOptions;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.map.Layer;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;

import de.micromata.opengis.kml.v_2_2_0.Feature;

/**
 * A class used by {@link KmlDecorator} to get the current encoding context (request, map content,
 * current layer, feature and so on).
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class KmlEncodingContext {
    
    boolean kmz;

    WMSMapContent mapContent;

    GetMapRequest request;

    List<Symbolizer> currentSymbolizers;

    Layer currentLayer;

    SimpleFeatureCollection currentFeatureCollection;

    SimpleFeature currentFeature;

    Map<String, Object> metadata = new HashMap<String, Object>();

    boolean descriptionEnabled;

    FeatureTemplate template = new FeatureTemplate();

    LookAtOptions lookAtOptions;

    WMS wms;
    
    Map<String, Layer> kmzGroundOverlays = new LinkedHashMap<String, Layer>();

    public KmlEncodingContext(WMSMapContent mapContent, WMS wms, boolean kmz) {
        super();
        this.mapContent = mapContent;
        this.request = mapContent.getRequest();
        this.wms = wms;
        this.descriptionEnabled = KMLUtils.getKMAttr(request, wms);
        this.lookAtOptions = new LookAtOptions(request.getFormatOptions());
        this.kmz = kmz;
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
    
    

}
