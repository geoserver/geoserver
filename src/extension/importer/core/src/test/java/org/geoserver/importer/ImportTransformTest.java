/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.importer.transform.DateFormatTransform;
import org.geoserver.importer.transform.IntegerFieldToDateTransform;
import org.geoserver.importer.transform.NumberFormatTransform;
import org.geoserver.importer.transform.ReprojectTransform;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class ImportTransformTest extends ImporterTestSupport {

    DataStoreInfo store;

    @Before
    public void setupStore() {
        Catalog cat = getCatalog();

        store = cat.getFactory().createDataStore();
        store.setWorkspace(cat.getDefaultWorkspace());
        store.setName("spearfish");
        store.setType("H2");

        Map params = new HashMap();
        params.put("database", getTestData().getDataDirectoryRoot().getPath() + "/spearfish");
        params.put("dbtype", "h2");
        store.getConnectionParameters().putAll(params);
        store.setEnabled(true);
        cat.add(store);
    }

    @After
    public void dropStore() {
        Catalog cat = getCatalog();
        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(cat);
        store.accept(visitor);
    }

    @Test
    public void testNumberFormatTransform() throws Exception {
        Catalog cat = getCatalog();

        File dir = unpack("shape/restricted.zip");

        SpatialFile file = new SpatialFile(new File(dir, "restricted.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file, store);
        assertEquals(1, context.getTasks().size());

        context.setTargetStore(store);

        ImportTask task = context.getTasks().get(0);
        task.getTransform().add(new NumberFormatTransform("cat", Integer.class));
        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());

        FeatureTypeInfo ft = cat.getFeatureTypeByDataStore(store, "restricted");
        assertNotNull(ft);

        SimpleFeatureType schema = (SimpleFeatureType) ft.getFeatureType();
        assertEquals(Integer.class, schema.getDescriptor("cat").getType().getBinding());

        FeatureIterator it = ft.getFeatureSource(null, null).getFeatures().features();
        try {
            assertTrue(it.hasNext());
            while (it.hasNext()) {
                SimpleFeature f = (SimpleFeature) it.next();
                assertTrue(f.getAttribute("cat") instanceof Integer);
            }
        } finally {
            it.close();
        }
    }

    @Test
    public void testIntegerToDateTransform() throws Exception {
        Catalog cat = getCatalog();

        File dir = unpack("shape/archsites_epsg_prj.zip");

        SpatialFile file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file, store);
        assertEquals(1, context.getTasks().size());

        context.setTargetStore(store);

        ImportTask task = context.getTasks().get(0);
        // this is a silly test - CAT_ID ranges from 1-25 and is not supposed to be a date
        // java date handling doesn't like dates in year 1
        task.getTransform().add(new IntegerFieldToDateTransform("CAT_ID"));
        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());

        FeatureTypeInfo ft = cat.getFeatureTypeByDataStore(store, "archsites");
        assertNotNull(ft);

        SimpleFeatureType schema = (SimpleFeatureType) ft.getFeatureType();
        assertEquals(Timestamp.class, schema.getDescriptor("CAT_ID").getType().getBinding());

        FeatureIterator it = ft.getFeatureSource(null, null).getFeatures().features();
        int year = 2;
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            // make sure we have something
            assertTrue(it.hasNext());
            // the first date will be bogus due to java date limitation
            it.next();
            while (it.hasNext()) {
                SimpleFeature f = (SimpleFeature) it.next();
                // class will be timestamp
                cal.setTime((Date) f.getAttribute("CAT_ID"));
                assertEquals(year++, cal.get(Calendar.YEAR));
            }
        } finally {
            it.close();
        }
    }

    @Test
    public void testDateFormatTransform() throws Exception {
        Catalog cat = getCatalog();

        File dir = unpack("shape/ivan.zip");

        SpatialFile file = new SpatialFile(new File(dir, "ivan.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file, store);
        assertEquals(1, context.getTasks().size());

        context.setTargetStore(store);

        ImportTask task = context.getTasks().get(0);
        task.getTransform().add(new DateFormatTransform("timestamp", "yyyy-MM-dd HH:mm:ss.S"));

        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());

        FeatureTypeInfo ft = cat.getFeatureTypeByDataStore(store, "ivan");
        assertNotNull(ft);

        SimpleFeatureType schema = (SimpleFeatureType) ft.getFeatureType();
        assertTrue(
                Date.class.isAssignableFrom(
                        schema.getDescriptor("timestamp").getType().getBinding()));

        FeatureIterator it = ft.getFeatureSource(null, null).getFeatures().features();
        try {
            assertTrue(it.hasNext());
            while (it.hasNext()) {
                SimpleFeature f = (SimpleFeature) it.next();
                assertTrue(f.getAttribute("timestamp") instanceof Date);
            }
        } finally {
            it.close();
        }
    }

    @Test
    public void testReprojectTransform() throws Exception {
        Catalog cat = getCatalog();

        File dir = unpack("shape/archsites_epsg_prj.zip");

        SpatialFile file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file, store);
        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());

        LayerInfo l1 = context.getTasks().get(0).getLayer();
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        CRS.decode("EPSG:26713"), l1.getResource().getNativeCRS()));
        assertEquals("EPSG:26713", l1.getResource().getSRS());

        dir = unpack("shape/archsites_epsg_prj.zip");

        file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();

        context = importer.createContext(file, store);
        ImportTask item = context.getTasks().get(0);
        item.getTransform().add(new ReprojectTransform(CRS.decode("EPSG:4326")));
        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());

        LayerInfo l2 = context.getTasks().get(0).getLayer();
        assertTrue(
                CRS.equalsIgnoreMetadata(CRS.decode("EPSG:4326"), l2.getResource().getNativeCRS()));
        assertEquals("EPSG:4326", l2.getResource().getSRS());

        assertFalse(
                l1.getResource()
                        .getNativeBoundingBox()
                        .equals(l2.getResource().getNativeBoundingBox()));
        assertTrue(
                CRS.equalsIgnoreMetadata(
                        l2.getResource().getNativeCRS(),
                        l2.getResource().getNativeBoundingBox().getCoordinateReferenceSystem()));

        LayerInfo l = cat.getLayer(l2.getId());
        assertTrue(
                CRS.equalsIgnoreMetadata(CRS.decode("EPSG:4326"), l2.getResource().getNativeCRS()));
        assertEquals("EPSG:4326", l2.getResource().getSRS());
    }
}
