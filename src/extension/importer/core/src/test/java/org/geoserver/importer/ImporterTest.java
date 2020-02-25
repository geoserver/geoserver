/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.FileSystemWatcher;
import org.geoserver.platform.resource.Resource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Before;
import org.junit.Test;

public class ImporterTest extends ImporterTestSupport {

    @Before
    public void addPrimitiveGeoFeature() throws IOException {
        revertLayer(SystemTestData.PRIMITIVEGEOFEATURE);
    }

    @Test
    public void testCreateContextSingleFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        SpatialFile file = new SpatialFile(new File(dir, "archsites.shp"));
        file.prepare();

        ImportContext context = importer.createContext(file);
        assertEquals(1, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(file, task.getData());
    }

    @Test
    public void testCreateContextDirectoryHomo() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        Directory d = new Directory(dir);
        ImportContext context = importer.createContext(d);
        assertEquals(2, context.getTasks().size());

        ImportTask task = context.getTasks().get(0);
        assertEquals(d.part("archsites"), task.getData());

        task = context.getTasks().get(1);
        assertEquals(d.part("bugsites"), task.getData());
    }

    @Test
    public void testCreateContextDirectoryHetero() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("geotiff/EmissiveCampania.tif.bz2", dir);

        Directory d = new Directory(dir);

        ImportContext context = importer.createContext(d);
        assertEquals(2, context.getTasks().size());

        // cannot ensure order of tasks due to hashing
        HashSet files = new HashSet();
        files.add(context.getTasks().get(0).getData());
        files.add(context.getTasks().get(1).getData());
        assertTrue(files.containsAll(d.getFiles()));
    }

    @Test
    public void testCreateContextFromArchive() throws Exception {
        File file = file("shape/archsites_epsg_prj.zip");
        Archive arch = new Archive(file);

        ImportContext context = importer.createContext(arch);
        assertEquals(1, context.getTasks().size());
    }

    @Test
    public void testCreateContextIgnoreHidden() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        FileUtils.touch(new File(dir, ".DS_Store"));

        ImportContext context = importer.createContext(new Directory(dir));
        assertEquals(1, context.getTasks().size());
    }

    @Test
    public void testCalculateBounds() throws Exception {

        FeatureTypeInfo resource = getCatalog().getFeatureTypeByName("sf", "PrimitiveGeoFeature");
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        ReferencedEnvelope nativeBounds = cb.getNativeBounds(resource);
        resource.setNativeBoundingBox(nativeBounds);
        resource.setLatLonBoundingBox(cb.getLatLonBounds(nativeBounds, resource.getCRS()));
        getCatalog().save(resource);

        assertNotNull(resource.getNativeBoundingBox());
        assertFalse(resource.getNativeBoundingBox().isEmpty());

        ReferencedEnvelope bbox = resource.getNativeBoundingBox();

        // Test null bbox
        resource.setNativeBoundingBox(null);
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertEquals(bbox, resource.getNativeBoundingBox());

        // Test empty bbox
        resource.setNativeBoundingBox(new ReferencedEnvelope());
        assertTrue(resource.getNativeBoundingBox().isEmpty());
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertEquals(bbox, resource.getNativeBoundingBox());

        // Test nonempty bbox - should not be changed
        ReferencedEnvelope customBbox =
                new ReferencedEnvelope(30, 60, -10, 30, bbox.getCoordinateReferenceSystem());
        resource.setNativeBoundingBox(customBbox);
        assertFalse(bbox.equals(resource.getNativeBoundingBox()));
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertFalse(bbox.equals(resource.getNativeBoundingBox()));

        // Test with "recalculate-bounds"=false
        resource.setNativeBoundingBox(customBbox);
        resource.getMetadata().put("recalculate-bounds", false);
        assertFalse(bbox.equals(resource.getNativeBoundingBox()));
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertFalse(bbox.equals(resource.getNativeBoundingBox()));

        // Test with "recalculate-bounds"=true
        resource.setNativeBoundingBox(customBbox);
        resource.getMetadata().put("recalculate-bounds", true);
        assertFalse(bbox.equals(resource.getNativeBoundingBox()));
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertTrue(bbox.equals(resource.getNativeBoundingBox()));

        // Test with "recalculate-bounds"="true"
        resource.setNativeBoundingBox(customBbox);
        resource.getMetadata().put("recalculate-bounds", "true");
        assertFalse(bbox.equals(resource.getNativeBoundingBox()));
        importer.calculateBounds(resource);
        assertFalse(resource.getNativeBoundingBox().isEmpty());
        assertTrue(bbox.equals(resource.getNativeBoundingBox()));
    }

    @Test
    public void testImporterConfiguration() throws Exception {
        // schedule for shorter delays
        ((FileSystemWatcher) getResourceLoader().getResourceNotificationDispatcher())
                .schedule(10, TimeUnit.MILLISECONDS);

        // update the configuration
        Resource props = getDataDirectory().get("importer/importer.properties");
        ImporterInfoDAO dao = new ImporterInfoDAO();
        ImporterInfo config = new ImporterInfoImpl();
        config.setMaxAsynchronousImports(5);
        config.setMaxSynchronousImports(7);
        dao.write(config, props);

        // forcing the importer to reload manually, as we don't know how fast the polling thread
        // will be able to catch up
        importer.reloadConfiguration();

        // make sure the importer picked up the change
        assertEquals(5, importer.asynchronousJobs.getMaximumPoolSize());
        assertEquals(7, importer.synchronousJobs.getMaximumPoolSize());
    }
}
