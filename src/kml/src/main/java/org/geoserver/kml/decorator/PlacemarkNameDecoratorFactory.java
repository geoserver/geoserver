/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
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
import org.geotools.styling.SLD;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;

/**
 * Template driven decorator setting the name in Placemark objects
 *
 * @author Andrea Aime - GeoSolutions
 */
public class PlacemarkNameDecoratorFactory implements KmlDecoratorFactory {

    @Override
    public KmlDecorator getDecorator(
            Class<? extends Feature> featureClass, KmlEncodingContext context) {
        if (Placemark.class.isAssignableFrom(featureClass) && context.isDescriptionEnabled()) {
            return new PlacemarkNameDecorator();
        } else {
            return null;
        }
    }

    static class PlacemarkNameDecorator implements KmlDecorator {
        static final Logger LOGGER = Logging.getLogger(PlacemarkNameDecorator.class);

        @Override
        public Feature decorate(Feature feature, KmlEncodingContext context) {
            Placemark pm = (Placemark) feature;

            // try with the template
            SimpleFeature sf = context.getCurrentFeature();
            String title = null;
            try {
                title = context.getTemplate().title(sf);
            } catch (IOException e) {
                String msg = "Error occured processing 'title' template.";
                LOGGER.log(Level.WARNING, msg, e);
            }

            // if we got nothing, set the title to the ID, but also try the text symbolizers
            String featureId = sf.getID();
            if (title == null || "".equals(title) || featureId.equals(title)) {
                title = featureId;

                // see if we can do better with a text symbolizer
                // symbolizers are available only in wms mode
                if (context.getCurrentSymbolizers() != null) {
                    StringBuffer label = new StringBuffer();
                    for (Symbolizer sym : context.getCurrentSymbolizers()) {
                        if (sym instanceof TextSymbolizer) {
                            Expression e = SLD.textLabel((TextSymbolizer) sym);
                            if (e != null) {
                                String value = e.evaluate(sf, String.class);

                                if ((value != null) && !"".equals(value.trim())) {
                                    label.append(value);
                                }
                            }
                        }
                    }

                    if (label.length() > 0) {
                        title = label.toString();
                    }
                }
            }

            pm.setName(title);
            return pm;
        }
    }
}
