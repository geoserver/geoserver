package org.geoserver.kml.decorator;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.wms.featureinfo.FeatureTemplate;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

public class PlacemarkDescriptionDecoratorFactory implements KmlDecoratorFactory {

    @Override
    public KmlDecorator getDecorator(Class<? extends Feature> featureClass,
            KmlEncodingContext context) {
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
                description = template.description(sf).toString();
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
