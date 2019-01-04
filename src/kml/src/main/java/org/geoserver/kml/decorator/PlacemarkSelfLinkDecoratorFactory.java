/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.decorator;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.atom.Link;
import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.logging.Logging;

/**
 * Adds an atom link used by the GeoSearch extension TODO: move this to the GeoSearch module
 *
 * @author Andrea Aime - GeoSolutions
 */
public class PlacemarkSelfLinkDecoratorFactory implements KmlDecoratorFactory {

    @Override
    public KmlDecorator getDecorator(
            Class<? extends Feature> featureClass, KmlEncodingContext context) {
        // this decorator makes sense only for WMS
        if (!(context.getService() instanceof WMSInfo)) {
            return null;
        }

        String selfLinks = (String) context.getRequest().getFormatOptions().get("selfLinks");
        if (selfLinks != null
                && selfLinks.equalsIgnoreCase("true")
                && Placemark.class.isAssignableFrom(featureClass)) {
            return new PlacemarkSelfLinkDecorator();
        } else {
            return null;
        }
    }

    static class PlacemarkSelfLinkDecorator extends AbstractGeoSearchDecorator {
        static final Logger LOGGER = Logging.getLogger(PlacemarkSelfLinkDecorator.class);

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
