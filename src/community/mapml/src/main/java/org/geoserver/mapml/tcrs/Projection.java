/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

/*
 * The Projection class supplies proj4j projection services for known projections
 */
public class Projection {

    private final CoordinateReferenceSystem proj4CRS;
    private static final CRSFactory factory = new CRSFactory();

    public Projection(String proj4code) {
        this.proj4CRS = factory.createFromName(proj4code);
    }

    public Point project(LatLng latlng) {
        ProjCoordinate src = new ProjCoordinate(latlng.lng, latlng.lat);
        ProjCoordinate dest = new ProjCoordinate();
        proj4CRS.getProjection().project(src, dest);
        return new Point(dest.x, dest.y);
    }

    public LatLng unproject(Point p) {
        ProjCoordinate src = new ProjCoordinate(p.x, p.y);
        ProjCoordinate dest = new ProjCoordinate();
        proj4CRS.getProjection().inverseProject(src, dest);
        return new LatLng(dest.y, dest.x);
    }
}
