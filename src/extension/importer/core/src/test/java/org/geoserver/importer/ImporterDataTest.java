/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.importer.ImportTask.State;
import org.geoserver.importer.transform.AbstractInlineVectorTransform;
import org.geoserver.importer.transform.AttributesToPointGeometryTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.h2.H2DataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;


public class ImporterDataTest extends ImporterTestSupport {

    private static final class DescriptionLimitingTransform extends AbstractInlineVectorTransform {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public SimpleFeature apply(ImportTask task, DataStore dataStore, SimpleFeature oldFeature,
                SimpleFeature feature) throws Exception {
            Object origDesc = feature.getAttribute("description");
            if (origDesc == null) {
                return feature;
            }
            String newDesc = StringUtils.abbreviate(origDesc.toString(), 255);
            feature.setAttribute("description", newDesc);
            return feature;
        }
    }
    
    @Test
    public void testImportShapefile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        
        ImportContext context = 
                importer.createContext(new SpatialFile(new File(dir, "archsites.shp")));
        assertEquals(1, context.getTasks().size());
        
        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());
        assertEquals("archsites", task.getLayer().getResource().getName());

        importer.run(context);
        
        Catalog cat = getCatalog();
        assertNotNull(cat.getLayerByName("archsites"));

        assertEquals(ImportTask.State.COMPLETE, task.getState());

        runChecks("archsites");
    }

    @Test
    public void testImportShapefilesWithExtraFiles() throws Exception {
        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        
        // make some 'extra' files
        new File(dir,"archsites.sbn").createNewFile();
        new File(dir,"archsites.sbx").createNewFile();
        new File(dir,"archsites.shp.xml").createNewFile();
        
        ImportContext context = importer.createContext(new Directory(dir));
        
        assertEquals(1, context.getTasks().size());
        
        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());
        assertEquals("archsites", task.getLayer().getResource().getName());
    }
    
    @Test
    public void testImportShapefiles() throws Exception {
        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        
        ImportContext context = importer.createContext(new Directory(dir));
        assertEquals(2, context.getTasks().size());
        
        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());
        assertEquals("archsites", task.getLayer().getResource().getName());

        task = context.getTasks().get(1);
        assertEquals(ImportTask.State.READY, task.getState());
        assertEquals("bugsites", task.getLayer().getResource().getName());

        importer.run(context);

        Catalog cat = getCatalog();
        assertNotNull(cat.getLayerByName("archsites"));
        assertNotNull(cat.getLayerByName("bugsites"));
        
        assertEquals(ImportTask.State.COMPLETE, context.getTasks().get(0).getState());
        assertEquals(ImportTask.State.COMPLETE, context.getTasks().get(1).getState());
        
        runChecks("archsites");
        runChecks("bugsites");
    }

    @Test
    public void testImportShapefilesWithError() throws Exception {
        File dir = tmpDir();
        unpack("shape/archsites_no_crs.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        ImportContext context = importer.createContext(new Directory(dir));
        assertEquals(2, context.getTasks().size());

        ImportTask task1 = context.getTasks().get(0);
        assertEquals(ImportTask.State.NO_CRS, task1.getState());
        assertEquals("archsites", task1.getLayer().getResource().getName());

        ImportTask task2 = context.getTasks().get(1);
        assertEquals(ImportTask.State.READY, task2.getState());
        assertEquals("bugsites", task2.getLayer().getResource().getName());

        importer.run(context);

        Catalog cat = getCatalog();
        assertNull(cat.getLayerByName("archsites"));
        assertNotNull(cat.getLayerByName("bugsites"));

        assertEquals(ImportTask.State.NO_CRS, task1.getState());
        assertEquals(ImportTask.State.COMPLETE, task2.getState());

        runChecks("bugsites");
    }
 
    @Test
    public void testImportNoCrsLatLonBoundingBox() throws Exception {
        File dir = unpack("shape/archsites_no_crs.zip");

        ImportContext context = importer.createContext(new Directory(dir));
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.NO_CRS, task.getState());
        assertNull(task.getLayer().getResource().getLatLonBoundingBox());

        task.getLayer().getResource().setSRS("EPSG:26713");
        importer.changed(task);

        assertEquals(ImportTask.State.READY, task.getState());

        ResourceInfo r = task.getLayer().getResource();
        assertNotNull(r.getLatLonBoundingBox());
        assertNotNull(r.boundingBox());
        assertNotNull(r.boundingBox().getCoordinateReferenceSystem());
    }

    @Test
    public void testImportUnknownFile() throws Exception {
        File dir = unpack("gml/states_wfs11.xml.gz");

        ImportContext context = importer.createContext(new Directory(dir)); 
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.NO_FORMAT, task.getState());
        assertNull(task.getData().getFormat());
    }

    @Test
    public void testImportUnknownFileIndirect() throws Exception {
        
        DataStoreInfo ds = createH2DataStore(null, "foo");
        File dir = unpack("gml/states_wfs11.xml.gz");
        ImportContext context = importer.createContext(new Directory(dir), ds); 
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.NO_FORMAT, task.getState());
        assertNull(task.getData().getFormat());
    }

    @Test
    public void testImportDatabase() throws Exception {
        File dir = unpack("h2/cookbook.zip");

        Map params = new HashMap();
        params.put(H2DataStoreFactory.DBTYPE.key, "h2");
        params.put(H2DataStoreFactory.DATABASE.key, new File(dir, "cookbook").getAbsolutePath());
     
        ImportContext context = importer.createContext(new Database(params));
        assertEquals(3, context.getTasks().size());

        assertEquals(ImportTask.State.READY, context.getTasks().get(0).getState());
        assertEquals(ImportTask.State.READY, context.getTasks().get(1).getState());
        assertEquals(ImportTask.State.READY, context.getTasks().get(2).getState());
        
        Catalog cat = getCatalog();
        assertNull(cat.getDataStoreByName(cat.getDefaultWorkspace(), "cookbook"));
        assertNull(cat.getLayerByName("point"));
        assertNull(cat.getLayerByName("line"));
        assertNull(cat.getLayerByName("polygon"));

        importer.run(context);
        assertEquals(ImportTask.State.COMPLETE, context.getTasks().get(0).getState());
        assertEquals(ImportTask.State.COMPLETE, context.getTasks().get(1).getState());
        assertEquals(ImportTask.State.COMPLETE, context.getTasks().get(2).getState());
        
        assertNotNull(cat.getDataStoreByName(cat.getDefaultWorkspace(), "cookbook"));

        DataStoreInfo ds = cat.getDataStoreByName(cat.getDefaultWorkspace(), "cookbook");
        assertNotNull(cat.getFeatureTypeByDataStore(ds, "point"));
        assertNotNull(cat.getFeatureTypeByDataStore(ds, "line"));
        assertNotNull(cat.getFeatureTypeByDataStore(ds, "polygon"));
        assertNotNull(cat.getLayerByName("point"));
        assertNotNull(cat.getLayerByName("line"));
        assertNotNull(cat.getLayerByName("polygon"));

        runChecks("point");
        runChecks("line");
        runChecks("polygon");
    }
//    
////    public void testImportGML() throws Exception {
////        File dir = unpack("gml/states_wfs11.gml.gz");
////        
////        Import imp = importer.newImport();
////        imp.setSource(new SpatialFile(new File(dir, "states_wfs11.gml"), new GMLFormat()));
////
////        importer.prepare(imp);
////        assertEquals(ImportStatus.READY, imp.getStatus());
////        assertEquals(1, imp.getLayers().size());
////        assertEquals(LayerStatus.READY, imp.getLayers().get(0).getStatus());
////
////        importer.run(imp);
////        
////        //converting gml leaves us without a crs and without bounds
////        assertTrue(imp.getLayers(LayerStatus.COMPLETED).isEmpty());
////        assertEquals(1, imp.getLayers().size());
////        assertEquals(LayerStatus.NO_CRS, imp.getLayers().get(0).getStatus());
////        imp.getLayers().get(0).setCRS(CRS.decode("EPSG:4326"));
////        
////        importer.run(imp);
////        runChecks("states");
////    }
//    
//    public void testImportGMLWithPrjFile() throws Exception {
//        File dir = unpack("gml/states_wfs11_prj.zip");
//        
//        Import imp = importer.newImport();
//        imp.setSource(new Directory(dir));
//
//        importer.prepare(imp);
//        assertEquals(ImportStatus.READY, imp.getStatus());
//        assertEquals(1, imp.getLayers().size());
//        assertEquals(LayerStatus.READY, imp.getLayers().get(0).getStatus());
//
//        importer.run(imp);
//        
//        runChecks("states");
//    }
//

    @Test
    public void testImportIntoDatabase() throws Exception {
        Catalog cat = getCatalog();

        DataStoreInfo ds = createH2DataStore(cat.getDefaultWorkspace().getName(), "spearfish");

        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        ImportContext context = importer.createContext(new Directory(dir), ds);
        assertEquals(2, context.getTasks().size());

        ImportTask task1 = context.getTasks().get(0);
        ImportTask task2 = context.getTasks().get(1);
        
        assertEquals(ImportTask.State.READY, task1.getState());
        assertEquals(ImportTask.State.READY, task2.getState());
        //assertEquals(ImportTask.State.READY, context.getTasks().get(1).getState());
        
        // cannot ensure ordering of items
        HashSet resources = new HashSet();
        resources.add(task1.getLayer().getResource().getName());
        resources.add(task2.getLayer().getResource().getName());
        assertTrue(resources.contains("bugsites"));
        assertTrue(resources.contains("archsites"));

        importer.run(context);

        assertEquals(ImportTask.State.COMPLETE, task1.getState());
        assertEquals(ImportTask.State.COMPLETE, task2.getState());

        assertNotNull(cat.getLayerByName("archsites"));
        assertNotNull(cat.getLayerByName("bugsites"));

        assertNotNull(cat.getFeatureTypeByDataStore(ds, "archsites"));
        assertNotNull(cat.getFeatureTypeByDataStore(ds, "bugsites"));

        runChecks("archsites");
        runChecks("bugsites");
    }
    
    @Test
    public void testImportIntoDatabaseWithEncoding() throws Exception {
        Catalog cat = getCatalog();

        DataStoreInfo ds = createH2DataStore(cat.getDefaultWorkspace().getName(), "ming"); 

        File dir = tmpDir();
        unpack("shape/ming_time.zip", dir);

        ImportContext context = importer.createContext(new Directory(dir), ds);
        assertEquals(1, context.getTasks().size());

        context.getTasks().get(0).getData().setCharsetEncoding("UTF-8");
        importer.run(context);
        
        FeatureTypeInfo info = (FeatureTypeInfo) context.getTasks().get(0).getLayer().getResource();
        FeatureSource<? extends FeatureType, ? extends Feature> fs = info.getFeatureSource(null, null);
        FeatureCollection<? extends FeatureType, ? extends Feature> features = fs.getFeatures();
        FeatureIterator<? extends Feature> it = features.features();
        assertTrue(it.hasNext());
        SimpleFeature next = (SimpleFeature) it.next();
        // let's test some attributes to see if they were digested properly
        String type_ch = (String) next.getAttribute("type_ch");
        assertEquals("卫",type_ch);
        String name_ch = (String) next.getAttribute("name_ch");
        assertEquals("杭州前卫",name_ch);
        
        it.close();
    }
    
    @Test
    public void testImportIntoDatabaseUpdateModes() throws Exception {
        testImportIntoDatabase();
        
        DataStoreInfo ds = getCatalog().getDataStoreByName("spearfish");
        assertNotNull(ds);
        
        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        
        FeatureSource<? extends FeatureType, ? extends Feature> fs = getCatalog().getFeatureTypeByName("archsites").getFeatureSource(null, null);
        int archsitesCount = fs.getCount(Query.ALL);
        fs = getCatalog().getFeatureTypeByName("bugsites").getFeatureSource(null, null);
        int bugsitesCount = fs.getCount(Query.ALL);

        ImportContext context = importer.createContext(new Directory(dir), ds);
        context.getTasks().get(0).setUpdateMode(UpdateMode.REPLACE);
        context.getTasks().get(1).setUpdateMode(UpdateMode.APPEND);
        
        importer.run(context);
        
        fs = getCatalog().getFeatureTypeByName("archsites").getFeatureSource(null, null);
        int archsitesCount2 = fs.getCount(Query.ALL);
        fs = getCatalog().getFeatureTypeByName("bugsites").getFeatureSource(null, null);
        int bugsitesCount2 = fs.getCount(Query.ALL);
        
        // tasks might not be in same order
        if (context.getTasks().get(0).getLayer().getName().equals("archsites")) {
            assertEquals(archsitesCount, archsitesCount2);
            assertEquals(bugsitesCount * 2, bugsitesCount2);
        } else {
            assertEquals(archsitesCount * 2, archsitesCount2);
            assertEquals(bugsitesCount, bugsitesCount2);
        }
    }

    @Test
    public void testImportGeoTIFF() throws Exception {
        File dir = unpack("geotiff/EmissiveCampania.tif.bz2");
        
        ImportContext context = 
                importer.createContext(new SpatialFile(new File(dir, "EmissiveCampania.tif")));
        assertEquals(1, context.getTasks().size());
        
        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());
        
        assertEquals("EmissiveCampania", task.getLayer().getResource().getName());

        importer.run(context);
        
        Catalog cat = getCatalog();
        assertNotNull(cat.getLayerByName("EmissiveCampania"));

        assertEquals(ImportTask.State.COMPLETE, task.getState());

        runChecks("EmissiveCampania");
    }

//    public void testUnknownFormat() throws Exception {
//        File dir = unpack("gml/states_wfs11.xml.gz");
//        
//        Import imp = importer.newImport();
//        imp.setSource(new SpatialFile(new File(dir, "states_wfs11.xml"), new GMLFormat()));
//
//        importer.prepare(imp);
//        assertEquals(ImportStatus.READY, imp.getStatus());
//        assertEquals(1, imp.getLayers().size());
//        assertEquals(LayerStatus.READY, imp.getLayers().get(0).getStatus());
//
//        importer.run(imp);
//        
//        //converting gml leaves us without a crs and without bounds
//        assertTrue(imp.getCompleted().isEmpty());
//        assertEquals(1, imp.getLayers().size());
//        assertEquals(LayerStatus.NO_CRS, imp.getLayers().get(0).getStatus());
//        imp.getLayers().get(0).setCRS(CRS.decode("EPSG:4326"));
//        
//        importer.run(imp);
//        runChecks("states");
//    }

    @Test
    public void testImportNameClash() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        
        ImportContext context = 
                importer.createContext(new SpatialFile(new File(dir, "archsites.shp")));
        importer.run(context);
        
        Catalog cat = getCatalog();
        assertNotNull(cat.getLayerByName("archsites"));
        runChecks("archsites");

        context = importer.createContext(new SpatialFile(new File(dir, "archsites.shp")));
        importer.run(context);

        assertEquals("archsites0", context.getTasks().get(0).getLayer().getName());
        runChecks("archsites0");
    }

    @Test
    public void testArchiveOnIndirectImport() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        assertTrue(dir.exists());

        DataStoreInfo ds = createH2DataStore(null, "foo");
        
        ImportContext context = 
            importer.createContext(new SpatialFile(new File(dir, "archsites.shp")), ds);
        context.setArchive(true);
        importer.run(context);
        assertFalse(dir.exists());

        dir = unpack("shape/bugsites_esri_prj.tar.gz");
        assertTrue(dir.exists());
        context = importer.createContext(new SpatialFile(new File(dir, "bugsites.shp")), ds);
        context.setArchive(false);

        importer.run(context);
        assertTrue(dir.exists());
    }

    @Test
    public void testImportDatabaseIntoDatabase() throws Exception {
        File dir = unpack("h2/cookbook.zip");

        DataStoreInfo ds = createH2DataStore("gs", "cookbook");

        Map params = new HashMap();
        params.put(H2DataStoreFactory.DBTYPE.key, "h2");
        params.put(H2DataStoreFactory.DATABASE.key, new File(dir, "cookbook").getAbsolutePath());
     
        ImportContext context = importer.createContext(new Database(params), ds);
        assertEquals(3, context.getTasks().size());
    }

    @Test
    public void testImportCSV() throws Exception {
        File dir = unpack("csv/locations.zip");
        ImportContext context = importer.createContext(new SpatialFile(new File(dir,
                "locations.csv")));
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.NO_CRS, task.getState());
        
        LayerInfo layer = task.getLayer();
        ResourceInfo resource = layer.getResource();
        resource.setSRS("EPSG:4326");

        assertTrue("Item not ready", importer.prep(task));
        assertEquals(ImportTask.State.READY, task.getState());
        
        context.updated();
        assertEquals(ImportContext.State.PENDING, context.getState());
        importer.run(context);
        assertEquals(ImportContext.State.COMPLETE, context.getState());
        FeatureTypeInfo fti = (FeatureTypeInfo) resource;
        SimpleFeatureType featureType = (SimpleFeatureType) fti.getFeatureType();
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        assertNull("Expecting no geometry", geometryDescriptor);
        assertEquals(4, featureType.getAttributeCount());
    }

    @Test
    public void testImportCSVIndirect() throws Exception {
        File dir = unpack("csv/locations.zip");
        String wsName = getCatalog().getDefaultWorkspace().getName();
        
        DataStoreInfo h2DataStore = createH2DataStore(wsName, "csvindirecttest");
        SpatialFile importData = new SpatialFile(new File(dir, "locations.csv"));

        ImportContext context = importer.createContext(importData, h2DataStore);
        assertEquals(1, context.getTasks().size());
        ImportTask task = context.getTasks().get(0);
        
        TransformChain transformChain = task.getTransform();
        transformChain.add(new AttributesToPointGeometryTransform("LAT", "LON"));
        assertEquals(ImportTask.State.NO_CRS, task.getState());
        
        LayerInfo layer = task.getLayer();
        ResourceInfo resource = layer.getResource();
        resource.setSRS("EPSG:4326");
        
        assertTrue("Item not ready", importer.prep(task));
        assertEquals(ImportTask.State.READY, task.getState());

        context.updated();
        assertEquals(ImportContext.State.PENDING, context.getState());
        importer.run(context);

        assertEquals(ImportContext.State.COMPLETE, context.getState());
        FeatureTypeInfo fti = (FeatureTypeInfo) resource;
        SimpleFeatureType featureType = (SimpleFeatureType) fti.getFeatureType();
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        assertNotNull("Expecting geometry", geometryDescriptor);
        assertEquals("Invalid geometry name", "location", geometryDescriptor.getLocalName());
        assertEquals(3, featureType.getAttributeCount());
        FeatureSource<? extends FeatureType, ? extends Feature> featureSource = fti.getFeatureSource(null, null);
        FeatureCollection<? extends FeatureType, ? extends Feature> features = featureSource.getFeatures();
        assertEquals(9, features.size());
        FeatureIterator<? extends Feature> featureIterator = features.features();
        assertTrue("Expected features", featureIterator.hasNext());
        SimpleFeature feature = (SimpleFeature) featureIterator.next();
        assertNotNull(feature);
        assertEquals("Invalid city attribute", "Trento", feature.getAttribute("CITY"));
        assertEquals("Invalid number attribute", 140, feature.getAttribute("NUMBER"));
        Object geomAttribute = feature.getAttribute("location");
        assertNotNull("Expected geometry", geomAttribute);
        Point point = (Point) geomAttribute;
        Coordinate coordinate = point.getCoordinate();
        assertEquals("Invalid x coordinate", 11.12, coordinate.x, 0.1);
        assertEquals("Invalid y coordinate", 46.07, coordinate.y, 0.1);
        featureIterator.close();
    }

    @Test
    public void testImportKMLIndirect() throws Exception {
        File dir = unpack("kml/sample.zip");
        String wsName = getCatalog().getDefaultWorkspace().getName();
        DataStoreInfo h2DataStore = createH2DataStore(wsName, "kmltest");
        SpatialFile importData = new SpatialFile(new File(dir, "sample.kml"));
        ImportContext context = importer.createContext(importData, h2DataStore);
        assertEquals(1, context.getTasks().size());
        ImportTask task = context.getTasks().get(0);

        LayerInfo layer = task.getLayer();
        ResourceInfo resource = layer.getResource();
        assertEquals("Invalid srs", "EPSG:4326", resource.getSRS());
        ReferencedEnvelope emptyBounds = new ReferencedEnvelope();
        emptyBounds.setToNull();
        assertTrue("Unexpected bounding box", emptyBounds.equals(resource.getNativeBoundingBox()));
        // transform chain to limit characters
        // otherwise we get a sql exception thrown
        TransformChain transformChain = task.getTransform();
        transformChain.add(new DescriptionLimitingTransform());
        importer.run(context);
        Exception error = task.getError();
        if (error != null) {
            error.printStackTrace();
            fail(error.getMessage());
        }
        assertFalse("Bounding box not updated", emptyBounds.equals(resource.getNativeBoundingBox()));
        FeatureTypeInfo fti = (FeatureTypeInfo) resource;
        assertEquals("Invalid type name", "sample", fti.getName());
        FeatureSource<? extends FeatureType, ? extends Feature> featureSource = fti
                .getFeatureSource(null, null);
        assertEquals("Unexpected feature count", 20, featureSource.getCount(Query.ALL));
    }

    @Test
    public void testImportDirectoryWithRasterIndirect() throws Exception {
        
        DataStoreInfo ds = createH2DataStore(getCatalog().getDefaultWorkspace().getName(), "shapes");

        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        unpack("geotiff/EmissiveCampania.tif.bz2", dir);
        
        ImportContext context = importer.createContext(new Directory(dir), ds);
        assertEquals(3, context.getTasks().size());
        assertTrue(context.getData() instanceof Directory);

        ImportTask task = Iterables.find(context.getTasks(), new Predicate<ImportTask>() {
            @Override
            public boolean apply(ImportTask input) {
                return "archsites".equals(input.getLayer().getResource().getName());
            }
        });
        assertEquals(ImportTask.State.READY, task.getState());
        assertTrue(task.getData() instanceof SpatialFile);
        assertEquals("Shapefile", task.getData().getFormat().getName());
        
        task = Iterables.find(context.getTasks(), new Predicate<ImportTask>() {
            @Override
            public boolean apply(ImportTask input) {
                return "bugsites".equals(input.getLayer().getResource().getName());
            }
        });
        assertEquals(ImportTask.State.READY, task.getState());
        
        assertTrue(task.getData() instanceof SpatialFile);
        assertEquals("Shapefile", task.getData().getFormat().getName());
        
        task = Iterables.find(context.getTasks(), new Predicate<ImportTask>() {
            @Override
            public boolean apply(ImportTask input) {
                return "EmissiveCampania".equals(input.getLayer().getResource().getName());
            }
        });
        assertEquals(ImportTask.State.BAD_FORMAT, task.getState());
        assertTrue(task.getData() instanceof SpatialFile);
        assertEquals("GeoTIFF", task.getData().getFormat().getName());
    }
    
    @Test
    public void testImportDirectoryWithRasterDirect() throws Exception {
        File dir = tmpDir();
        unpack("shape/archsites_epsg_prj.zip", dir);
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        unpack("geotiff/EmissiveCampania.tif.bz2", dir);
        
        ImportContext context = importer.createContext(new Directory(dir));
        assertEquals(3, context.getTasks().size());
        assertTrue(context.getData() instanceof Directory);

        ImportTask task = context.getTasks().get(0);
        assertEquals(ImportTask.State.READY, task.getState());
        assertEquals("archsites", task.getLayer().getResource().getName());
        assertTrue(task.getData() instanceof SpatialFile);
        assertEquals("Shapefile", task.getData().getFormat().getName());
        
        task = context.getTasks().get(1);
        assertEquals(ImportTask.State.READY, task.getState());
        assertEquals("bugsites", task.getLayer().getResource().getName());
        assertTrue(task.getData() instanceof SpatialFile);
        assertEquals("Shapefile", task.getData().getFormat().getName());
        
        task = context.getTasks().get(2);
        assertEquals(ImportTask.State.READY, task.getState());
        assertEquals("EmissiveCampania", task.getLayer().getResource().getName());
        assertTrue(task.getData() instanceof SpatialFile);
        assertEquals("GeoTIFF", task.getData().getFormat().getName());
    }

    @Test
    public void testGeoJSONImport() throws Exception {
        DataStoreInfo h2 = 
            createH2DataStore(getCatalog().getDefaultWorkspace().getName(), "jsontest");

        File dir = unpack("geojson/point.json.zip");
        ImportContext imp = importer.createContext(
            new SpatialFile(new File(dir, "point.json")), h2);

        assertEquals(1, imp.getTasks().size());
        assertEquals(ImportTask.State.READY, imp.task(0).getState());

        importer.run(imp);

        assertEquals(ImportContext.State.COMPLETE, imp.getState());
        checkNoErrors(imp);

        runChecks("point");
    }

    @Test
    public void testJaggedGeoJSON() throws Exception {
        File json = file("geojson/jagged.json");
        ImportContext ctx = importer.createContext(new SpatialFile(json));
        assertEquals(1, ctx.getTasks().size());
        SimpleFeatureType info = (SimpleFeatureType) ctx.getTasks().get(0).getMetadata().get(FeatureType.class);
        assertEquals(4, info.getAttributeCount());
        int cnt = 0;
        for (int i = 0; i < info.getAttributeCount(); i++) {
            if (info.getDescriptor(i).getLocalName().equals("geometry")) {
                cnt++;
            }
        }
        assertEquals(1, cnt);
    }

    private void checkNoErrors(ImportContext imp) {
        for (ImportTask task : imp.getTasks()) {
            assertNull(task.getError());
            assertEquals(State.COMPLETE, task.getState());
        }
    }

    @Test
    public void testGeoJSONImportDirectory() throws Exception {
        DataStoreInfo h2 = 
            createH2DataStore(getCatalog().getDefaultWorkspace().getName(), "jsontest");

        File dir = unpack("geojson/point.json.zip");
        unpack("geojson/line.json.zip", dir);
        unpack("geojson/polygon.json.zip", dir);

        ImportContext imp = importer.createContext(new Directory(dir), h2);
        assertEquals(3, imp.getTasks().size());
        
        assertEquals(ImportContext.State.PENDING, imp.getState());
        assertEquals(ImportTask.State.READY, imp.task(0).getState());
        assertEquals(ImportTask.State.READY, imp.task(1).getState());
        assertEquals(ImportTask.State.READY, imp.task(2).getState());

        importer.run(imp);

        assertEquals(ImportContext.State.COMPLETE, imp.getState());

        runChecks("point");
        runChecks("line");
        runChecks("polygon");
    }

    @Test
    public void testIllegalNames() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        for (File f : dir.listFiles()) {
            String ext = FilenameUtils.getExtension(f.getName());
            String base = FilenameUtils.getBaseName(f.getName());

            f.renameTo(new File(dir, "1-." + ext));
        }

        ImportContext imp = importer.createContext(new Directory(dir));
        importer.run(imp);

        ImportTask task = imp.getTasks().get(0);

        assertEquals("a_1_", task.getLayer().getName());
        assertEquals("a_1_", task.getLayer().getResource().getName());
    }
}
