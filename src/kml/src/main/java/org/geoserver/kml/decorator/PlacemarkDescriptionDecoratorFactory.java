/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Adds template based description to Placemark objects
 *
 * @author Andrea Aime - GeoSolutions
 */
public class PlacemarkDescriptionDecoratorFactory implements KmlDecoratorFactory {

    @Override
    public KmlDecorator getDecorator(
            Class<? extends Feature> featureClass, KmlEncodingContext context) {
        // this decorator makes sense only for WMS
        if (!(context.getService() instanceof WMSInfo)) {
            return null;
        }

        if (Placemark.class.isAssignableFrom(featureClass) && context.isDescriptionEnabled()) {
            return new PlacemarkDescriptionDecorator();
        } else {
            return null;
        }
    }

    static class PlacemarkDescriptionDecorator implements KmlDecorator {
        static final Logger LOGGER = Logging.getLogger(PlacemarkDescriptionDecorator.class);

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            FeatureTemplate template = context.getTemplate();
            SimpleFeature sf = context.getCurrentFeature();
            String description = null;
            try {
                description = template.description(sf);
            } catch (IOException e) {
                String msg = "Error occured processing 'description' template.";
                LOGGER.log(Level.WARNING, msg, e);
            }

            Placemark pm = (Placemark) feature;
            if (description != null) {
                pm.setDescription(description);
            }
            return pm;
        }
    }
}
