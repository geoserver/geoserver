/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.sequence;

import java.util.List;

import org.geoserver.kml.KMLUtils;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.kml.decorator.KmlEncodingContext;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;

/**
 * Creates a sequence of folders mapping the layers in the map content
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class FolderSequenceFactory implements SequenceFactory<Feature> {

    private KmlEncodingContext context;
    private List<KmlDecorator> decorators;

    public FolderSequenceFactory(KmlEncodingContext context) {
        this.context = context;
        this.decorators = context.getDecoratorsForClass(Folder.class);
    }

    @Override
    public Sequence<Feature> newSequence() {
        return new FolderGenerator();
    }

    public class FolderGenerator implements Sequence<Feature> {
        int i = 0;

        int size;

        public FolderGenerator() {
            this.size = context.getMapContent().layers().size();
        }

        @Override
        public Feature next() {
            while (i < size) {
                List<Layer> layers = context.getMapContent().layers();
                Layer layer = layers.get(i++);
                context.setCurrentLayer(layer);
                
                if(layer instanceof FeatureLayer) {
                    try {
                        WMSMapContent mapContent = context.getMapContent();
                        SimpleFeatureCollection fc = KMLUtils.loadFeatureCollection(
                                (SimpleFeatureSource) layer.getFeatureSource(), layer, mapContent, context.getWms(),
                                mapContent.getScaleDenominator());
                        context.setCurrentFeatureCollection(fc);
                    } catch (Exception e) {
                        if (e instanceof ServiceException) {
                            throw (ServiceException) e;
                        } else {
                            throw new ServiceException("Failed to load vector data during KML generation", e);
                        }
                    }
                }

                // setup the folder and let it be decorated
                Folder folder = new Folder();
                folder.setName(layer.getTitle());
                for (KmlDecorator decorator : decorators) {
                    folder = (Folder) decorator.decorate(folder, context);
                    if(folder == null) {
                        continue;
                    }
                }
                
                if (layer instanceof FeatureLayer) {
                    List<Feature> features = new SequenceList<Feature>(new FeatureSequenceFactory(context, (FeatureLayer) layer));
                    List<Feature> originalFeatures = folder.getFeature();
                    if(originalFeatures == null || originalFeatures.size() == 0) {
                        folder.setFeature(features);
                    } else {
                        // in this case, compose the already existing features with the dynamically
                        // generated ones
                        folder.setFeature(new CompositeList<Feature>(originalFeatures, features));
                    }
                }

                return folder;
            } 
            return null;
        }

    }

}
