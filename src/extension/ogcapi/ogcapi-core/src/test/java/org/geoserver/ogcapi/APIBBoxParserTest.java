/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84_3D;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.geometry.BoundingBox;
import org.geotools.api.referencing.FactoryException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class APIBBoxParserTest {

    private static final double EPS = 0001;
    public static final String BBOX_3D_SPEC = "10,20,5,30,40,15";
    public static final String BBOX_2D_SPEC = "10,20,30,40";
    private static PropertyName DEFAULT_GEOMETRY =
            CommonFactoryFinder.getFilterFactory().property("");

    @Test
    public void testParse2DBBox() throws FactoryException {
        ReferencedEnvelope[] result = APIBBoxParser.parse(BBOX_2D_SPEC);

        assertNotNull(result);
        assertEquals(1, result.length);
        ReferencedEnvelope envelope = result[0];
        assertEnvelope(envelope, 10, 20, 30, 40);
        assertEquals(DefaultGeographicCRS.WGS84, envelope.getCoordinateReferenceSystem());
    }

    @Test
    public void testToFilter2DBBox() throws FactoryException {
        Filter boxFilter = APIBBoxParser.toFilter(BBOX_2D_SPEC);

        assertNotNull(boxFilter);
        assertThat(boxFilter, instanceOf(BBOX.class));
        BBOX bboxFilter = (BBOX) boxFilter;
        assertEnvelope(bboxFilter.getBounds(), 10, 20, 30, 40);
        assertEquals(DefaultGeographicCRS.WGS84, bboxFilter.getBounds().getCoordinateReferenceSystem());
        assertEquals(DEFAULT_GEOMETRY, bboxFilter.getExpression1());
    }

    @Test
    public void testParse3DBBox() throws FactoryException {
        ReferencedEnvelope[] result = APIBBoxParser.parse(BBOX_3D_SPEC);

        assertNotNull(result);
        assertEquals(1, result.length);
        ReferencedEnvelope envelope = result[0];
        assertEnvelope(envelope, 10, 20, 5, 30, 40, 15);
        assertEquals(WGS84_3D, envelope.getCoordinateReferenceSystem());
    }

    @Test
    public void testToFilter3DBBox() throws FactoryException {
        Filter boxFilter = APIBBoxParser.toFilter(BBOX_3D_SPEC);

        assertNotNull(boxFilter);
        assertThat(boxFilter, instanceOf(BBOX.class));
        BBOX bboxFilter = (BBOX) boxFilter;
        assertEnvelope(bboxFilter.getBounds(), 10, 20, 5, 30, 40, 15);
        assertEquals(WGS84_3D, bboxFilter.getBounds().getCoordinateReferenceSystem());
        assertEquals(DEFAULT_GEOMETRY, bboxFilter.getExpression1());
    }

    @Test
    public void testParse2DBBoxWithCRS() throws FactoryException {
        String bbox = BBOX_2D_SPEC;
        String crs = "EPSG:4326";
        ReferencedEnvelope[] result = APIBBoxParser.parse(bbox, crs);

        assertNotNull(result);
        assertEquals(1, result.length);
        ReferencedEnvelope envelope = result[0];
        assertEnvelope(envelope, 10, 20, 30, 40);
        assertNotNull(envelope.getCoordinateReferenceSystem());
        assertEquals(crs, CRS.lookupIdentifier(envelope.getCoordinateReferenceSystem(), false));
    }

    @Test
    public void testParseDatelineSpan() throws FactoryException {
        ReferencedEnvelope[] result = APIBBoxParser.parse("160, 20, -160, 40");

        assertNotNull(result);
        assertEquals(2, result.length);
        assertEnvelope(result[0], 160, 20, 180, 40);
        assertEquals(DefaultGeographicCRS.WGS84, result[0].getCoordinateReferenceSystem());
        assertEnvelope(result[1], -180, 20, -160, 40);
        assertEquals(DefaultGeographicCRS.WGS84, result[1].getCoordinateReferenceSystem());
    }

    @Test
    public void testFilterDatelineSpan() throws FactoryException {
        Filter filter = APIBBoxParser.toFilter("160, 20, -160, 40");

        assertThat(filter, instanceOf(Or.class));
        Or or = (Or) filter;
        assertThat(or.getChildren().get(0), instanceOf(BBOX.class));
        assertThat(or.getChildren().get(1), instanceOf(BBOX.class));

        BBOX bbox1 = (BBOX) or.getChildren().get(0);
        assertEnvelope(bbox1.getBounds(), 160, 20, 180, 40);
        assertEquals(DefaultGeographicCRS.WGS84, bbox1.getBounds().getCoordinateReferenceSystem());
        assertEquals(DEFAULT_GEOMETRY, bbox1.getExpression1());

        BBOX bbox2 = (BBOX) or.getChildren().get(1);
        assertEnvelope(bbox2.getBounds(), -180, 20, -160, 40);
        assertEquals(DefaultGeographicCRS.WGS84, bbox2.getBounds().getCoordinateReferenceSystem());
        assertEquals(DEFAULT_GEOMETRY, bbox2.getExpression1());
    }

    @Test
    public void testToGeometryDatelineSpan() throws FactoryException {
        Geometry geometry = APIBBoxParser.toGeometry("160, 20, -160, 40");

        assertThat(geometry, instanceOf(MultiPolygon.class));
        Polygon p1 = (Polygon) geometry.getGeometryN(0);
        assertTrue(p1.isRectangle());
        assertEquals(new Envelope(160, 180, 20, 40), p1.getEnvelopeInternal());
        Polygon p2 = (Polygon) geometry.getGeometryN(1);
        assertTrue(p2.isRectangle());
        assertEquals(new Envelope(-180, -160, 20, 40), p2.getEnvelopeInternal());
    }

    @Test
    public void testParse3DBBoxWithCRS() throws FactoryException {
        String bbox = BBOX_3D_SPEC;
        String crs = "EPSG:4326";
        ReferencedEnvelope[] result = APIBBoxParser.parse(bbox, crs);

        assertNotNull(result);
        assertEquals(1, result.length);
        ReferencedEnvelope envelope = result[0];
        assertEnvelope(envelope, 10, 20, 5, 30, 40, 15);
        assertNotNull(envelope.getCoordinateReferenceSystem());
        assertEquals(crs, CRS.lookupIdentifier(envelope.getCoordinateReferenceSystem(), false));
    }

    private void assertEnvelope(BoundingBox bounds, double... expected) {
        assertEnvelope(ReferencedEnvelope.reference(bounds), expected);
    }

    private static void assertEnvelope(ReferencedEnvelope envelope, double... expected) {
        if (expected.length == 6) {
            ReferencedEnvelope3D envelope3D = (ReferencedEnvelope3D) envelope;
            assertEquals(expected[0], envelope3D.getMinX(), EPS);
            assertEquals(expected[1], envelope3D.getMinY(), EPS);
            assertEquals(expected[2], envelope3D.getMinZ(), EPS);
            assertEquals(expected[3], envelope3D.getMaxX(), EPS);
            assertEquals(expected[4], envelope3D.getMaxY(), EPS);
            assertEquals(expected[5], envelope3D.getMaxZ(), EPS);
        } else {
            assertEquals(expected[0], envelope.getMinX(), EPS);
            assertEquals(expected[1], envelope.getMinY(), EPS);
            assertEquals(expected[2], envelope.getMaxX(), EPS);
            assertEquals(expected[3], envelope.getMaxY(), EPS);
        }
    }
}
