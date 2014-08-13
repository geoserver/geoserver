package org.geoserver.geosearch.rest;

import org.geoserver.kml.KmlEncodingContext;

import de.micromata.opengis.kml.v_2_2_0.Kml;

final class KmlEncodingBundle {
    Kml kml;
    KmlEncodingContext context;

    public KmlEncodingBundle(Kml kml, KmlEncodingContext context) {
        this.kml = kml;
        this.context = context;
    }
    
}