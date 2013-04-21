/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.kml.GeoSearchKMLTest;
import org.geoserver.kml.KMLUtils;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.atom.Link;

/**
 * Adds an atom link used by the GeoSearch extension 
 * TODO: move this to the GeoSearch module
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class PlacemarkSelfLinkDecoratorFactory implements KmlDecoratorFactory {

    @Override
    public KmlDecorator getDecorator(Class<? extends Feature> featureClass,
            KmlEncodingContext context) {
        String selfLinks = (String) context.getRequest().getFormatOptions().get("selfLinks");
        if (selfLinks != null && selfLinks.equalsIgnoreCase("true") && Placemark.class.isAssignableFrom(featureClass)) {
            return new PlacemarkSeltLinkDecorator();
        } else {
            return null;
        }
    }

    static class PlacemarkSeltLinkDecorator extends AbstractGeoSearchDecorator {
        static final Logger LOGGER = Logging.getLogger(PlacemarkSeltLinkDecorator.class);

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            Placemark pm = (Placemark) feature;
            
            String link = "";

            try {
                link = getFeatureTypeURL(context);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            String[] id = context.getCurrentFeature().getID().split("\\.");

            link = link + "/" + id[1] + ".kml";

            Link al = pm.createAndSetAtomLink(link);
            al.setRel("self");
            
            return pm;
        }

    }

}
