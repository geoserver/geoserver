/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.sequence;

import java.util.List;

import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.kml.decorator.KmlEncodingContext;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

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
                    folder.setFeature(features);
                }

                return folder;
            } 
            return null;
        }

    }

}
