/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test for WFS GetFeature OutputFormat for GeoPackage
 *
 * @author Niels Charlier
 */
public class GeoPackageGetFeatureOutputFormatTest extends WFSTestSupport {

    protected static FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    protected GeoPackageGetFeatureOutputFormat format;

    protected Operation op;

    protected GetFeatureType gft;

    @Before
    public void init() {
        gft = WfsFactory.eINSTANCE.createGetFeatureType();
        format = new GeoPackageGetFeatureOutputFormat(getGeoServer());
        op = new Operation("GetFeature", getServiceDescriptor10(), null, new Object[] {gft});
    }

    @Test
    public void testGetFeatureOneType() throws IOException {
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());

        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                getFeatureSource(SystemTestData.BASIC_POLYGONS);
        ;
        fct.getFeature().add(fs.getFeatures());

        testGetFeature(fct, true);
    }

    @Test
    public void testGetFeatureTwoTypes() throws IOException {
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());

        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                getFeatureSource(SystemTestData.LAKES);
        fct.getFeature().add(fs.getFeatures());

        fs = getFeatureSource(SystemTestData.STREAMS);
        fct.getFeature().add(fs.getFeatures());

        testGetFeature(fct, true);
    }

    @Test
    public void testGetFeatureWithFilter() throws IOException {
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());

        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                getFeatureSource(SystemTestData.LAKES);
        fct.getFeature().add(fs.getFeatures());

        fs = getFeatureSource(SystemTestData.STREAMS);
        FeatureCollection coll =
                fs.getFeatures(ff.equals(ff.property("NAME"), ff.literal("Cam Stream")));
        assertEquals(1, coll.size());

        fct.getFeature().add(coll);
        testGetFeature(fct, true);
    }

    @Test
    public void testGetFeatureWithSpatialIndex() throws IOException {
        System.setProperty(GeoPackageGetFeatureOutputFormat.PROPERTY_INDEXED, "true");
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());

        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                getFeatureSource(SystemTestData.BASIC_POLYGONS);
        fct.getFeature().add(fs.getFeatures());

        testGetFeature(fct, true);

        System.getProperties().remove(GeoPackageGetFeatureOutputFormat.PROPERTY_INDEXED);
    }

    @Test
    public void testHttpStuff() throws Exception {
        String layerName = SystemTestData.BASIC_POLYGONS.getLocalPart();
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=getfeature&typename="
                                + layerName
                                + "&outputformat=geopackage");
        assertEquals(GeoPkg.MIME_TYPE, resp.getContentType());

        assertEquals(
                "attachment; filename=" + layerName + ".gpkg",
                resp.getHeader("Content-Disposition"));

        resp =
                getAsServletResponse(
                        "wfs?request=getfeature&typename="
                                + layerName
                                + "&outputformat=geopackage"
                                + "&format_options=filename:test");
        assertEquals(GeoPkg.MIME_TYPE, resp.getContentType());
        assertEquals("attachment; filename=test.gpkg", resp.getHeader("Content-Disposition"));

        resp =
                getAsServletResponse(
                        "wfs?request=getfeature&typename="
                                + layerName
                                + "&outputformat=geopackage"
                                + "&format_options=filename:TEST.GPKG");
        assertEquals(GeoPkg.MIME_TYPE, resp.getContentType());
        assertEquals("attachment; filename=TEST.GPKG", resp.getHeader("Content-Disposition"));
    }

    // if the FC already is XY, then forceXY does nothing (should return same FC)
    @Test
    public void testForceXY_alreadyXY() throws IOException {
        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                getFeatureSource(SystemTestData.BASIC_POLYGONS);
        SimpleFeatureCollection fc = (SimpleFeatureCollection) fs.getFeatures();

        SimpleFeatureCollection fcXY = GeoPackageGetFeatureOutputFormat.forceXY(fc);

        assertEquals(
                CRS.getAxisOrder(fc.getSchema().getCoordinateReferenceSystem()),
                CRS.AxisOrder.EAST_NORTH);
        assertSame(fc, fcXY);
    }

    // if underlying data is YX, result should be XY
    @Test
    public void testForceXY_simpleFlip() throws Exception {
        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                getFeatureSource(SystemTestData.BASIC_POLYGONS);
        SimpleFeatureCollection fc = (SimpleFeatureCollection) fs.getFeatures();

        // create a FeatureCollection that is advertised as YX
        String wkt_yx =
                "GEOGCS[\"WGS 84\", \n"
                        + "  DATUM[\"World Geodetic System 1984\", \n"
                        + "    SPHEROID[\"WGS 84\", 6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]], \n"
                        + "    AUTHORITY[\"EPSG\",\"6326\"]], \n"
                        + "  PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], \n"
                        + "  UNIT[\"degree\", 0.017453292519943295], \n"
                        + "  AXIS[\"Geodetic longitude\", NORTH], \n"
                        + "  AXIS[\"Geodetic latitude\", EAST], \n"
                        + "  AUTHORITY[\"EPSG\",\"4326\"]]";
        CoordinateReferenceSystem crs_yx = CRS.parseWKT(wkt_yx);
        assertEquals(CRS.getAxisOrder(crs_yx), CRS.AxisOrder.NORTH_EAST);

        SimpleFeatureType newType = DataUtilities.createSubType(fc.getSchema(), null, crs_yx);
        SimpleFeature sf = fc.features().next();
        ((Geometry) sf.getDefaultGeometry())
                .setUserData(
                        null); // clear out CRS from geometry (or ReprojectingFeatureCollection will
        // use old CRS)
        SimpleFeatureCollection collectionYX =
                DataUtilities.collection(DataUtilities.reType(newType, sf));

        // xform
        SimpleFeatureCollection fcXY = GeoPackageGetFeatureOutputFormat.forceXY(collectionYX);

        assertEquals(
                CRS.getAxisOrder(fc.getSchema().getCoordinateReferenceSystem()),
                CRS.AxisOrder.EAST_NORTH);

        SimpleFeature sfXY = fcXY.features().next();

        // verify geometry is actually XY
        Coordinate coordinate = ((Geometry) sf.getDefaultGeometry()).getCoordinate();
        Coordinate coordinateYX = ((Geometry) sfXY.getDefaultGeometry()).getCoordinate();

        assertEquals(coordinate.x, coordinateYX.y, 0);
        assertEquals(coordinate.y, coordinateYX.x, 0);
    }

    public void testGetFeature(FeatureCollectionResponse fct, boolean indexed) throws IOException {
        // FileOutputStream fos = new FileOutputStream(new File("/home/niels/Temp/geopkg.db"));
        // format.write(fct, fos, op);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        format.write(fct, os, op);

        GeoPackage geopkg = createGeoPackage(os.toByteArray());

        // compare all feature collections
        for (FeatureCollection collection : fct.getFeatures()) {
            FeatureEntry e = new FeatureEntry();
            e.setTableName(collection.getSchema().getName().getLocalPart());
            e.setGeometryColumn(
                    collection.getSchema().getGeometryDescriptor().getName().getLocalPart());

            SimpleFeatureReader reader = geopkg.reader(e, null, null);

            SimpleFeatureCollection sCollection = (SimpleFeatureCollection) collection;

            // spatial index
            assertEquals(indexed, geopkg.hasSpatialIndex(e));

            // compare type
            SimpleFeatureType type1 = reader.getFeatureType();
            SimpleFeatureType type2 = sCollection.getSchema();
            assertEquals(type1.getDescriptors().size(), type2.getDescriptors().size());
            for (int i = 0; i < type1.getDescriptors().size(); i++) {
                assertEquals(type1.getDescriptor(i).getName(), type2.getDescriptor(i).getName());
                assertEquals(type1.getDescriptor(i).getType(), type2.getDescriptor(i).getType());
            }

            // compare data
            MemoryFeatureCollection memCollection = new MemoryFeatureCollection(type2);
            while (reader.hasNext()) {
                memCollection.add(reader.next());
            }

            assertEquals(sCollection.size(), memCollection.size());

            SimpleFeatureIterator it = sCollection.features();
            while (it.hasNext()) {
                SimpleFeature sf = it.next();
                for (int i = 0; i < type1.getDescriptors().size(); i++) {
                    assertTrue(findFeatureAttribute(memCollection, i, sf.getAttribute(i)));
                }
            }

            reader.close();
        }

        geopkg.close();
    }

    protected boolean findFeatureAttribute(
            SimpleFeatureCollection collection, int indexProp, Object value) {
        SimpleFeatureIterator it = collection.features();
        while (it.hasNext()) {
            SimpleFeature sf = it.next();
            if (sf.getAttribute(indexProp).equals(value)) {
                return true;
            }
        }
        return false;
    }

    protected GeoPackage createGeoPackage(byte[] inMemory) throws IOException {

        File f = File.createTempFile("temp", ".gpkg", new File("target"));
        FileOutputStream fout = new FileOutputStream(f);
        fout.write(inMemory);
        fout.flush();
        fout.close();

        return new GeoPackage(f);
    }
}
