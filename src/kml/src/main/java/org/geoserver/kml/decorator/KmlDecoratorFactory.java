/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import org.geoserver.kml.KmlEncodingContext;

/**
 * Builds {@link KmlDecorator} instances based on a target KML Feature class and the current
 * encoding context
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface KmlDecoratorFactory {

    KmlDecorator getDecorator(Class<? extends Feature> featureClass, KmlEncodingContext context);

    /**
     * Decorates/alters the specified KML {@link Feature}
     *
     * @author Andrea Aime - GeoSolutions
     */
    public interface KmlDecorator {

        /**
         * Decorates/alters the specified feature. If the return value is null, the feature has to
         * be skipped and won't be encoded
         */
        public Feature decorate(Feature feature, KmlEncodingContext context);
    }
}
