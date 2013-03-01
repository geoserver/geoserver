/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.sequence;

import java.util.EmptyStackException;
import java.util.List;

import org.geoserver.kml.KMLUtils;
import org.geoserver.kml.ScaleStyleVisitor;
import org.geoserver.kml.SymbolizerCollector;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.kml.decorator.KmlEncodingContext;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

/**
 * Creates a sequence of Placemark objects mapping the vector contents of a layer
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class FeatureSequenceFactory implements SequenceFactory<Feature> {

    private SimpleFeatureCollection features;

    private List<KmlDecorator> callbacks;

    private Style simplified;

    private KmlEncodingContext context;

    public FeatureSequenceFactory(KmlEncodingContext context, FeatureLayer layer) {
        this.context = context;
        WMSMapContent mapContent = context.getMapContent();
        try {
            this.features = KMLUtils.loadFeatureCollection(
                    (SimpleFeatureSource) layer.getFeatureSource(), layer, mapContent, context.getWms(),
                    mapContent.getScaleDenominator());
        } catch (Exception e) {
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            } else {
                throw new ServiceException("Failed to load vector data during KML generation", e);
            }
        }

        // prepare the encoding context
        context.setCurrentLayer(layer);
        

        // prepare the callbacks
        callbacks = context.getDecoratorsForClass(Placemark.class);

        // prepare the style for this layer
        simplified = getSimplifiedStyle(mapContent, layer);
    }

    private Style getSimplifiedStyle(WMSMapContent mc, Layer layer) {
        ScaleStyleVisitor visitor = new ScaleStyleVisitor(mc.getScaleDenominator());
        try {
            layer.getStyle().accept(visitor);
            return (Style) visitor.getCopy();
        } catch (EmptyStackException e) {
            return null;
        }
    }

    @Override
    public Sequence<Feature> newSequence() {
        return new FeatureGenerator(simplified != null ? features.features() : null);
    }

    public class FeatureGenerator implements Sequence<Feature> {

        private SimpleFeatureIterator fi;

        public FeatureGenerator(SimpleFeatureIterator fi) {
            this.fi = fi;
        }

        @Override
        public Feature next() {
            // already reached the end?
            if (fi == null) {
                return null;
            }

            while (fi.hasNext()) {
                boolean featureRetrieved = false;
                try {
                    // grab the next feature, with a sentinel to tell us whether there was an
                    // exception
                    featureRetrieved = false;
                    SimpleFeature sf = (SimpleFeature) fi.next();
                    featureRetrieved = true;
                    context.setCurrentFeature(sf);

                    List<Symbolizer> symbolizers = getSymbolizers(simplified, sf);
                    if (symbolizers.size() == 0) {
                        continue;
                    }
                    context.setCurrentSymbolizers(symbolizers);

                    // only create the basic placemark here, the rest is delegated to decorators
                    Placemark pm = new Placemark();
                    pm.setId(sf.getID());

                    // call onto the decorators
                    for (KmlDecorator callback : callbacks) {
                        pm = (Placemark) callback.decorate(pm, context);
                        if (pm == null) {
                            // we have to skip this one
                            continue;
                        }
                    }

                    return pm;
                } finally {
                    if (!featureRetrieved) {
                        // an exception has occurred, release the feature iterator
                        fi.close();
                        fi = null;
                    }
                }
            }

            // did we reach the end just now?
            if (!fi.hasNext()) {
                fi.close();
            }
            return null;
        }

        private List<Symbolizer> getSymbolizers(Style style, SimpleFeature sf) {
            SymbolizerCollector collector = new SymbolizerCollector(sf);
            style.accept(collector);
            return collector.getSymbolizers();
        }

    }

}
