/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import static org.junit.Assert.*;

import org.junit.Test;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class ProjectionTest {
    private Projection proj;

    // this is necessary to avoid warnings during tests
    protected static final ApplicationContext APPLICATION_CONTEXT =
            new FileSystemXmlApplicationContext(
                    "file:"
                            + ProjectionTest.class
                                    .getClassLoader()
                                    .getResource("applicationContext.xml")
                                    .getFile());

    public ProjectionTest() {}

    @Test
    public void testWGS84() {
        proj = new Projection("urn:ogc:def:crs:OGC:1.3:CRS84");

        // a location
        LatLng latlng = new LatLng(45.398043, -75.70683);

        Point expected = new Point(-75.70683D, 45.398043D);

        // the location calculated
        Point actual = null;
        try {

            actual = proj.project(latlng);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during project operation");
        }

        assertEquals(expected.x, actual.x, 0.0001);
        assertEquals(expected.y, actual.y, 0.0001);

        // reverse the process
        LatLng unprojected = null;
        try {
            unprojected = proj.unproject(actual);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during unproject operation");
        }
        assertEquals(latlng.lat, unprojected.lat, 0.00000001);
        assertEquals(latlng.lng, unprojected.lng, 0.00000001);
    }

    @Test
    public void testWebMercator() {
        // Spherical/Web Mercator
        proj = new Projection("urn:x-ogc:def:crs:EPSG:3857");

        // a location
        LatLng latlng = new LatLng(45.398043, -75.70683);

        // the location above measured off  a Leaflet map:
        // if you go to leafletjs.com, and open the console
        // and type
        // map.options.crs.project(L.marker([ 45.398043,
        // -75.70683]).addTo(map).bindPopup('Baz').openPopup().getLatLng())
        // you will get this point which is my bus stop
        Point expected = new Point(-8427645.7651, 5684404.3994);

        // the location in meters calculated with proj4j
        Point actual = null;
        try {
            actual = proj.project(latlng);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during project operation");
        }

        assertEquals(expected.x, actual.x, 0.0001);
        assertEquals(expected.y, actual.y, 0.0001);

        // reverse the process
        LatLng unprojected = null;
        try {
            unprojected = proj.unproject(actual);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during unproject operation");
        }
        assertEquals(latlng.lat, unprojected.lat, 0.00000001);
        assertEquals(latlng.lng, unprojected.lng, 0.00000001);
    }

    @Test
    public void testLambertConformalConic() {

        // NRCan LCC proj4 parameters for Canada:
        // +proj=lcc +lat_1=49 +lat_2=77 +lat_0=49 +lon_0=-95 +x_0=0 +y_0=0 +ellps=GRS80
        // +datum=NAD83 +units=m +no_defs
        proj = new Projection("urn:x-ogc:def:crs:EPSG:3978");

        // a location
        LatLng latlng = new LatLng(45.398043, -75.70683);

        // the location above measured off  a Proj4Leaflet map:
        Point expected = new Point(1510675.3477557, -172566.0893862);

        // the location in meters calculated with proj4j
        Point actual = null;
        try {
            actual = proj.project(latlng);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during project operation");
        }

        assertEquals(expected.x, actual.x, 0.0000001);
        assertEquals(expected.y, actual.y, 0.0000001);

        // reverse the process
        LatLng unprojected = null;
        try {
            unprojected = proj.unproject(actual);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during unproject operation");
        }
        assertEquals(latlng.lat, unprojected.lat, 0.00000001);
        assertEquals(latlng.lng, unprojected.lng, 0.00000001);
    }

    @Test
    public void testPolarStereoGraphic() {

        proj = new Projection("urn:x-ogc:def:crs:EPSG:5936");

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
        Point actual = null;
        try {
            actual = proj.project(latlng);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during project operation");
        }

        assertEquals(expected.x, actual.x, 0.001);
        assertEquals(expected.y, actual.y, 0.001);

        // reverse the process
        LatLng unprojected = null;
        try {
            unprojected = proj.unproject(actual);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during unproject operation");
        }
        assertEquals(latlng.lat, unprojected.lat, 0.00000001);
        assertEquals(latlng.lng, unprojected.lng, 0.00000001);
    }
}
