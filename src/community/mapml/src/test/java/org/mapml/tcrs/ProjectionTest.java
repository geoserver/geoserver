/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import static org.junit.Assert.*;

import org.junit.Test;

/** @author Peter.Rushforth@canada.ca */
public class ProjectionTest {
    private Projection proj;

    public ProjectionTest() {}

    @Test
    public void testWebMercator() {
        // Spherical/Web Mercator
        proj = new Projection("EPSG:3857");

        // a location
        LatLng latlng = new LatLng(45.398043, -75.70683);

        // the location above measured off  a Leaflet map:
        Point expected = new Point(-8427645.7651, 5684404.3994);

        // the location in meters calculated with proj4j
        Point actual = proj.project(latlng);

        assertEquals(expected.x, actual.x, 0.0001);
        assertEquals(expected.y, actual.y, 0.0001);

        // reverse the process
        LatLng unprojected = proj.unproject(actual);
        assertEquals(latlng.lat, unprojected.lat, 0.00000001);
        assertEquals(latlng.lng, unprojected.lng, 0.00000001);
    }

    @Test
    public void testLambertConformalConic() {

        // NRCan LCC proj4 parameters for Canada:
        // +proj=lcc +lat_1=49 +lat_2=77 +lat_0=49 +lon_0=-95 +x_0=0 +y_0=0 +ellps=GRS80
        // +datum=NAD83 +units=m +no_defs
        proj = new Projection("EPSG:3978");

        // a location
        LatLng latlng = new LatLng(45.398043, -75.70683);

        // the location above measured off  a Proj4Leaflet map:
        Point expected = new Point(1510675.3477557, -172566.0893862);

        // the location in meters calculated with proj4j
        Point actual = proj.project(latlng);

        assertEquals(expected.x, actual.x, 0.0000001);
        assertEquals(expected.y, actual.y, 0.0000001);

        // reverse the process
        LatLng unprojected = proj.unproject(actual);
        assertEquals(latlng.lat, unprojected.lat, 0.00000001);
        assertEquals(latlng.lng, unprojected.lng, 0.00000001);
    }

    @Test
    public void testPolarStereoGraphic() {

        proj = new Projection("EPSG:5936");

        // a location
        LatLng latlng = new LatLng(75.576217, -41.060996);

        // the location above  - not measured off anything!  Will hopefully round trip...
        // the location above  - not measured off anything!  Will hopefully round trip...
        // NOTE since changing dependencies to org.locationtech, this point
        // has changed location, due to (I believe) different precision
        // for numbers used in the definition.  locationtech's definition
        // looks closer to the EPSG:5936 definition, which is referenced by
        // the MapML specification, so I am changing the test to reflect the
        // org.locationtech return value.
        Point expected = new Point(3522418.97238712, 2522398.6592665017);

        // the location in meters calculated with proj4j
        Point actual = proj.project(latlng);

        assertEquals(expected.x, actual.x, 0.001);
        assertEquals(expected.y, actual.y, 0.001);

        // reverse the process
        LatLng unprojected = proj.unproject(actual);
        assertEquals(latlng.lat, unprojected.lat, 0.00000001);
        assertEquals(latlng.lng, unprojected.lng, 0.00000001);
    }
}
