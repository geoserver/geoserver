/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

/**
 * A WebMap containing a KML document
 *
 * @author Andrea Aime - GeoSolutions
 */
public class KMLMap extends WebMap {

    Kml kml;

    KmlEncodingContext kmlEncodingContext;

    public KMLMap(
            WMSMapContent map, KmlEncodingContext kmlEncodingContext, Kml kml, String mimeType) {
        super(map);
        this.kml = kml;
        this.kmlEncodingContext = kmlEncodingContext;
        super.setMimeType(mimeType);
    }

    public Kml getKml() {
        return kml;
    }

    public KmlEncodingContext getKmlEncodingContext() {
        return kmlEncodingContext;
    }
}
