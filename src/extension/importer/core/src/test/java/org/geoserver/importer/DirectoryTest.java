/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import static org.geoserver.importer.ImporterTestUtils.unpack;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.importer.mosaic.Mosaic;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DirectoryTest {

    @Before
    public void setUp() {
        GeoServerExtensionsHelper.singleton(
                "spatialFileExtensionsProvider",
                new SpatialFileExtensionsProvider(),
                SupplementalFileExtensionsProvider.class);
    }

    @Test
    public void testMosaicAuxiliaryFiles() throws Exception {
        File unpack = ImporterTestUtils.unpack("mosaic/bm.zip");

        // all types of junk!
        String[] aux = {"aux", "rrd", "xml", "tif.aux.xml", "tfw"};
        File[] tifs = unpack.listFiles();
        for (File file : tifs) {
            for (String s : aux) {
                new File(unpack, file.getName().replace("tif", s)).createNewFile();
            }
        }

        Mosaic m = new Mosaic(unpack);
        m.prepare();

        Assert.assertEquals(4, m.getFiles().size());
        for (int i = 0; i < m.getFiles().size(); i++) {
            Assert.assertEquals("GeoTIFF", m.getFiles().get(1).getFormat().getName());
        }
        // make sure the junk was actually picked up
        for (FileData f : m.getFiles()) {
            Assert.assertEquals(aux.length, ((SpatialFile) f).getSuppFiles().size());
        }
    }

    @Test
    public void testSingleSpatialFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        Directory d = new Directory(dir);
        d.prepare();

        List<FileData> files = d.getFiles();

        Assert.assertEquals(1, files.size());
        Assert.assertTrue(files.get(0) instanceof SpatialFile);

        SpatialFile spatial = (SpatialFile) files.get(0);
        Assert.assertEquals("shp", FilenameUtils.getExtension(spatial.getFile().getName()));

        Assert.assertNotNull(spatial.getPrjFile().getName());
        Assert.assertEquals("prj", FilenameUtils.getExtension(spatial.getPrjFile().getName()));

        Assert.assertEquals(2, spatial.getSuppFiles().size());

        Set<String> exts = new HashSet<>(Arrays.asList("shx", "dbf"));
        for (File supp : spatial.getSuppFiles()) {
            exts.remove(FilenameUtils.getExtension(supp.getName()));
        }

        Assert.assertTrue(exts.isEmpty());
    }

    @Test
    public void testShapefileWithMacOSXDirectory() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        File osxDir = new File(dir, "__MACOSX");
        osxDir.mkdir();
        new File(osxDir, ".archsites.shp").createNewFile();
        new File(osxDir, ".archsites.dbf").createNewFile();
        new File(osxDir, ".archsites.prj").createNewFile();

        Directory d = new Directory(dir);
        d.prepare();

        Assert.assertNotNull(d.getFormat());
        Assert.assertEquals(DataStoreFormat.class, d.getFormat().getClass());
        List<FileData> files = d.getFiles();
        Assert.assertEquals(1, files.size());
        Assert.assertEquals(DataStoreFormat.class, files.get(0).getFormat().getClass());
    }

    @Test
    public void testShapefileWithExtraFiles() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");

        // 'extra' files
        new File(dir, "archsites.shp.xml").createNewFile();
        new File(dir, "archsites.sbx").createNewFile();
        new File(dir, "archsites.sbn").createNewFile();
        new File(dir, "archsites.shp.ed.lock").createNewFile();

        Directory d = new Directory(dir);
        d.prepare();

        Assert.assertNotNull(d.getFormat());
        Assert.assertEquals(DataStoreFormat.class, d.getFormat().getClass());
        List<FileData> files = d.getFiles();
        Assert.assertEquals(1, files.size());
        Assert.assertEquals(DataStoreFormat.class, files.get(0).getFormat().getClass());
    }

    @Test
    public void testMultipleSpatialFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);

        Directory d = new Directory(dir);
        d.prepare();

        Assert.assertEquals(2, d.getFiles().size());
        Assert.assertTrue(d.getFiles().get(0) instanceof SpatialFile);
        Assert.assertTrue(d.getFiles().get(1) instanceof SpatialFile);
    }

    @Test
    public void testMultipleSpatialASpatialFile() throws Exception {
        File dir = unpack("shape/archsites_epsg_prj.zip");
        unpack("shape/bugsites_esri_prj.tar.gz", dir);
        FileUtils.touch(new File(dir, "foo.txt")); // TODO: don't rely on alphabetical order

        Directory d = new Directory(dir);
        d.prepare();

        Assert.assertEquals(3, d.getFiles().size());
        Assert.assertTrue(d.getFiles().get(0) instanceof SpatialFile);
        Assert.assertTrue(d.getFiles().get(1) instanceof SpatialFile);
        Assert.assertTrue(d.getFiles().get(2) instanceof ASpatialFile);
    }
}
