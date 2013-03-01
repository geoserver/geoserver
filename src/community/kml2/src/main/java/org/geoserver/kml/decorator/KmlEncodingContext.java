package org.geoserver.kml.decorator;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.geotools.map.Layer;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;

import de.micromata.opengis.kml.v_2_2_0.Feature;

public class KmlEncodingContext {

    WMSMapContent mapContent;

    GetMapRequest request;

    List<Symbolizer> currentSymbolizers;

    Layer currentLayer;

    SimpleFeature currentFeature;

    Map<String, Object> metadata = new HashMap<String, Object>();

    boolean descriptionEnabled;

    FeatureTemplate template = new FeatureTemplate();

    LookAtOptions lookAtOptions;

    WMS wms;

    public KmlEncodingContext(WMSMapContent mapContent, GetMapRequest request, WMS wms) {
        super();
        this.mapContent = mapContent;
        this.request = request;
        this.wms = wms;
        this.descriptionEnabled = KMLUtils.getKMAttr(request, wms);
        this.lookAtOptions = new LookAtOptions(request.getFormatOptions());
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

}
