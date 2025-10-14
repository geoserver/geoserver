/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.translate.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.gsr.api.ServiceException;
import org.geoserver.gsr.model.feature.EditResult;
import org.geoserver.gsr.model.feature.Feature;
import org.geoserver.gsr.model.geometry.Polyline;
import org.geoserver.gsr.model.geometry.SpatialReferenceWKID;
import org.geoserver.gsr.translate.geometry.GeometryEncoder;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.feature.FeatureIterator;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;

public class FeatureDAOTest extends GeoServerSystemTestSupport {

    @Before
    public void revert() throws IOException {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        revertLayer(MockData.POINTS);
        revertLayer(MockData.MPOINTS);
        revertLayer(MockData.MLINES);
        revertLayer(MockData.MPOLYGONS);
        revertLayer(MockData.LINES);
        revertLayer(MockData.POLYGONS);
    }

    @Test
    public void testCreateFeature() throws IOException, ServiceException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        // create feature from scratch
        Polyline geom = new Polyline(
                new Double[][][] {{{500050.0, 499950.0}, {500150.0, 500050.0}}}, new SpatialReferenceWKID(32615));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "t0002");
        Feature feature = new Feature(geom, attributes, "1");

        EditResult result = FeatureDAO.createFeature(fti, FeatureDAO.featureStore(fti), feature);
        assertTrue(
                result.getError() == null ? "" : result.getError().getDetails().toString(), result.getSuccess());
        assertNull(
                result.getError() == null ? "" : result.getError().getDetails().toString(), result.getError());
        // TODO: Cleanly increment ids?
        // assertEquals(1L, result.getObjectId().longValue());
        assertNotNull(result.getObjectId());
        assertNotEquals(0L, (long) result.getObjectId());

        assertEquals(2, fti.getFeatureSource(null, null).getFeatures().size());

        FeatureIterator<? extends org.geotools.api.feature.Feature> iterator =
                fti.getFeatureSource(null, null).getFeatures().features();
        // Lines.0
        iterator.next();
        // reported id should be compatible with actual id
        assertEquals(
                FeatureEncoder.toGSRObjectId(iterator.next().getIdentifier().getID()), result.getObjectId());
    }

    @Test
    public void testCreateFeatureReproject() throws IOException, ServiceException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        // create feature from scratch
        Polyline nativeGeom = new Polyline(
                new Double[][][] {{{500050.0, 499950.0}, {500150.0, 500050.0}}}, new SpatialReferenceWKID(32615));
        Polyline geom = new Polyline(
                new Double[][][] {{{-92.9995492, 4.5231103}, {-92.9986478, 4.5240149}}},
                new SpatialReferenceWKID(4326));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "t0002");
        Feature feature = new Feature(geom, attributes, "1");

        EditResult result = FeatureDAO.createFeature(fti, FeatureDAO.featureStore(fti), feature);
        assertTrue(
                result.getError() == null ? "" : result.getError().getDetails().toString(), result.getSuccess());
        assertNull(
                result.getError() == null ? "" : result.getError().getDetails().toString(), result.getError());
        assertNotNull(result.getObjectId());
        assertNotEquals(0L, (long) result.getObjectId());

        assertEquals(2, fti.getFeatureSource(null, null).getFeatures().size());

        FeatureIterator<? extends org.geotools.api.feature.Feature> iterator =
                fti.getFeatureSource(null, null).getFeatures().features();
        // Lines.0
        iterator.next();
        // reported id should be compatible with actual id
        org.geotools.api.feature.Feature nativeFeature = iterator.next();
        assertEquals(FeatureEncoder.toGSRObjectId(nativeFeature.getIdentifier().getID()), result.getObjectId());
        // reprojection should occur
        LineString nativeGeomJTS =
                (LineString) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(
                nativeGeomJTS.getGeometryN(0).getCoordinates()[0].x, nativeGeom.getPaths()[0][0][0].doubleValue(), 0.1);
        assertEquals(
                nativeGeomJTS.getGeometryN(0).getCoordinates()[0].y, nativeGeom.getPaths()[0][0][1].doubleValue(), 0.1);
    }

    @Test
    public void testCreateFeatureMultiline() throws IOException, ServiceException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "MLines");
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        // create feature from scratch
        Polyline geom = new Polyline(
                new Double[][][] {{{500050.0, 499950.0}, {500150.0, 500050.0}}}, new SpatialReferenceWKID(32615));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "t0002");
        Feature feature = new Feature(geom, attributes, "1");

        EditResult result = FeatureDAO.createFeature(fti, FeatureDAO.featureStore(fti), feature);
        assertTrue(
                result.getError() == null ? "" : result.getError().getDetails().toString(), result.getSuccess());
        assertNull(
                result.getError() == null ? "" : result.getError().getDetails().toString(), result.getError());
        assertNotNull(result.getObjectId());
        assertNotEquals(0L, (long) result.getObjectId());

        assertEquals(2, fti.getFeatureSource(null, null).getFeatures().size());

        FeatureIterator<? extends org.geotools.api.feature.Feature> iterator =
                fti.getFeatureSource(null, null).getFeatures().features();
        // MLines.0
        iterator.next();
        // reported id should be compatible with actual id
        org.geotools.api.feature.Feature nativeFeature = iterator.next();
        assertEquals(FeatureEncoder.toGSRObjectId(nativeFeature.getIdentifier().getID()), result.getObjectId());
        // geom should be valid
        MultiLineString nativeGeomJTS =
                (MultiLineString) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(nativeGeomJTS.getGeometryN(0).getCoordinates()[0].x, geom.getPaths()[0][0][0].doubleValue(), 0.1);
        assertEquals(nativeGeomJTS.getGeometryN(0).getCoordinates()[0].y, geom.getPaths()[0][0][1].doubleValue(), 0.1);
    }

    @Test
    public void testUpdateFeature() throws IOException, ServiceException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        Polyline geom = new Polyline(
                new Number[][][] {{{500050.0, 499950.0}, {500150.0, 500050.0}}}, new SpatialReferenceWKID(32615));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(FeatureEncoder.OBJECTID_FIELD_NAME, 0L);
        attributes.put("id", "t0001");
        Feature feature = new Feature(geom, attributes, "1");

        EditResult result = FeatureDAO.updateFeature(fti, FeatureDAO.featureStore(fti), feature);
        assertNull(
                result.getError() == null ? "" : result.getError().getDetails().toString(), result.getError());
        assertTrue(
                result.getError() == null ? "" : result.getError().getDetails().toString(), result.getSuccess());
        assertNotNull(result.getObjectId());
        assertEquals(0L, (long) result.getObjectId());

        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        FeatureIterator<? extends org.geotools.api.feature.Feature> iterator =
                fti.getFeatureSource(null, null).getFeatures().features();
        // verify geometry matches
        Geometry nativeGeometry =
                (Geometry) iterator.next().getDefaultGeometryProperty().getValue();
        Polyline transformedGeometry =
                ((Polyline) (new GeometryEncoder().toRepresentation(nativeGeometry, geom.getSpatialReference())));
        assertTrue(
                "Expected '"
                        + Arrays.deepToString(geom.getPaths())
                        + "' but was '"
                        + Arrays.deepToString(transformedGeometry.getPaths())
                        + "'",
                Arrays.deepEquals(geom.getPaths(), transformedGeometry.getPaths()));
    }

    @Test
    public void testUpdateFeatureNotExists() throws IOException, ServiceException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        Polyline geom = new Polyline(
                new Number[][][] {{{500050.0, 499950.0}, {500150.0, 500050.0}}}, new SpatialReferenceWKID(32615));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(FeatureEncoder.OBJECTID_FIELD_NAME, 1L);
        Feature feature = new Feature(geom, attributes, "1");

        EditResult result = FeatureDAO.updateFeature(fti, FeatureDAO.featureStore(fti), feature);
        assertFalse(result.getSuccess());
        assertNotNull(result.getError());
        assertNotNull(result.getObjectId());
        assertEquals(1L, (long) result.getObjectId());

        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        FeatureIterator<? extends org.geotools.api.feature.Feature> iterator =
                fti.getFeatureSource(null, null).getFeatures().features();
        // verify geometry matches
        Geometry nativeGeometry =
                (Geometry) iterator.next().getDefaultGeometryProperty().getValue();
        Polyline transformedGeometry =
                ((Polyline) (new GeometryEncoder().toRepresentation(nativeGeometry, geom.getSpatialReference())));
        assertFalse(
                "Expected '"
                        + Arrays.deepToString(geom.getPaths())
                        + "' but was '"
                        + Arrays.deepToString(transformedGeometry.getPaths())
                        + "'",
                Arrays.deepEquals(geom.getPaths(), transformedGeometry.getPaths()));
    }

    @Test
    public void testDeleteFeature() throws IOException, ServiceException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Points");
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        EditResult result = FeatureDAO.deleteFeature(fti, FeatureDAO.featureStore(fti), 0L);

        assertTrue(result.getSuccess());
        assertNull(result.getError());
        assertEquals(0L, result.getObjectId().longValue());

        assertEquals(0, fti.getFeatureSource(null, null).getFeatures().size());
    }

    @Test
    public void testDeleteFeatureNotExists() throws IOException, ServiceException {
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Points");
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        EditResult result = FeatureDAO.deleteFeature(fti, FeatureDAO.featureStore(fti), 42L);

        assertFalse(result.getSuccess());
        assertNotNull(result.getError());
        assertEquals(42L, result.getObjectId().longValue());

        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
    }
}
