/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.builder;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.util.List;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.kml.iterator.IteratorFactory;
import org.geoserver.kml.iterator.IteratorList;
import org.geoserver.kml.iterator.PlainFolderIteratorFactory;
import org.geoserver.platform.ServiceException;

/**
 * Builds a lazily evaluated KML document given a encoding context
 *
 * @author Andrea Aime - GeoSolutions
 */
public class StreamingKMLBuilder {

    /** Builds a lazily evaluated KML document given a encoding context */
    public Kml buildKMLDocument(KmlEncodingContext context) {
        // prepare kml, document and folder
        Kml kml = new Kml();
        Document document = kml.createAndSetDocument();
        String kmltitle = (String) context.getRequest().getFormatOptions().get("kmltitle");
        document.setName(kmltitle);

        // get the callbacks for the document and let them loose
        List<KmlDecorator> decorators = context.getDecoratorsForClass(Document.class);
        for (KmlDecorator decorator : decorators) {
            document = (Document) decorator.decorate(document, context);
            if (document == null) {
                throw new ServiceException(
                        "Coding error in decorator "
                                + decorator
                                + ", document objects cannot be set to null");
            }
        }

        // create a generator that will generate a folder and feature dumps/ground overlays for each
        // layer
        IteratorFactory<Feature> generatorFactory = new PlainFolderIteratorFactory(context);
        IteratorList<Feature> folders = new IteratorList<Feature>(generatorFactory);
        context.addFeatures(document, folders);

        return kml;
    }
}
