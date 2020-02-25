/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.geoserver.importer.ImporterTestUtils.unpack;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.importer.mosaic.Mosaic;

public class DirectoryTest extends TestCase {

    public void testMosaicAuxillaryFiles() throws Exception {
        File unpack = ImporterTestUtils.unpack("mosaic/bm.zip");

        // all types of junk!
        String[] aux = new String[] {"aux", "rrd", "xml", "tif.aux.xml", "tfw"};
        File[] tifs = unpack.listFiles();
        for (int i = 0; i < tifs.length; i++) {
            File file = tifs[i];
            for (int j = 0; j < aux.length; j++) {
                new File(unpack, file.getName().replace("tif", aux[j])).createNewFile();
            }
        }

        Mosaic m = new Mosaic(unpack);
        m.prepare();

        assertEquals(4, m.getFiles().size());
        for (int i = 0; i < m.getFiles().size(); i++) {
            assertEquals("GeoTIFF", m.getFiles().get(1).getFormat().getName());
        }
        // make sure the junk was actually picked up
        for (FileData f : m.getFiles()) {
            assertEquals(aux.length, ((SpatialFile) f).getSuppFiles().size());
        }
    }

    public void testSingleSpatialFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        Directory d = new Directory(dir);
        d.prepare();

        List<FileData> files = d.getFiles();

        assertEquals(1, files.size());
        assertTrue(files.get(0) instanceof SpatialFile);

        SpatialFile spatial = (SpatialFile) files.get(0);
        assertEquals("shp", FilenameUtils.getExtension(spatial.getFile().getName()));

        assertNotNull(spatial.getPrjFile().getName());
        assertEquals("prj", FilenameUtils.getExtension(spatial.getPrjFile().getName()));

        assertEquals(2, spatial.getSuppFiles().size());

        Set<String> exts = new HashSet<String>(Arrays.asList("shx", "dbf"));
        for (File supp : spatial.getSuppFiles()) {
            exts.remove(FilenameUtils.getExtension(supp.getName()));
        }

        assertTrue(exts.isEmpty());
    }

    public void testShapefileWithMacOSXDirectory() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        File osxDir = new File(dir, "__MACOSX");
        osxDir.mkdir();
        new File(osxDir, ".archsites.shp").createNewFile();
        new File(osxDir, ".archsites.dbf").createNewFile();
        new File(osxDir, ".archsites.prj").createNewFile();

        Directory d = new Directory(dir);
        d.prepare();

        assertNotNull(d.getFormat());
        assertEquals(DataStoreFormat.class, d.getFormat().getClass());
        List<FileData> files = d.getFiles();
        assertEquals(1, files.size());
        assertEquals(DataStoreFormat.class, files.get(0).getFormat().getClass());
    }

    public void testShapefileWithExtraFiles() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        // 'extra' files
        new File(dir, "archsites.shp.xml").createNewFile();
        new File(dir, "archsites.sbx").createNewFile();
        new File(dir, "archsites.sbn").createNewFile();
        new File(dir, "archsites.shp.ed.lock").createNewFile();

        Directory d = new Directory(dir);
        d.prepare();

        assertNotNull(d.getFormat());
        assertEquals(DataStoreFormat.class, d.getFormat().getClass());
        List<FileData> files = d.getFiles();
        assertEquals(1, files.size());
        assertEquals(DataStoreFormat.class, files.get(0).getFormat().getClass());
    }

    public void testMultipleSpatialFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        Directory d = new Directory(dir);
        d.prepare();

        assertEquals(2, d.getFiles().size());
        assertTrue(d.getFiles().get(0) instanceof SpatialFile);
        assertTrue(d.getFiles().get(1) instanceof SpatialFile);
    }

    public void testMultipleSpatialASpatialFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        FileUtils.touch(new File(dir, "foo.txt")); // TODO: don't rely on alphabetical order

        Directory d = new Directory(dir);
        d.prepare();

        assertEquals(3, d.getFiles().size());
        assertTrue(d.getFiles().get(0) instanceof SpatialFile);
        assertTrue(d.getFiles().get(1) instanceof SpatialFile);
        assertTrue(d.getFiles().get(2) instanceof ASpatialFile);
    }
}
