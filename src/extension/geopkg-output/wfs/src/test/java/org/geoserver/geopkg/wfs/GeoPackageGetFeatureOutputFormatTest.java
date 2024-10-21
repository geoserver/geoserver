/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.WfsFactory;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geopkg.GeoPkg;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.SimpleFeatureReader;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

        fct.getFeature().add(fs.getFeatures());

        validateGetFeature(fct, true);
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

        validateGetFeature(fct, true);
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
        validateGetFeature(fct, true);
    }

    @Test
    public void testGetFeatureWithSpatialIndex() throws IOException {
        System.setProperty(GeoPackageGetFeatureOutputFormat.PROPERTY_INDEXED, "true");
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());

        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                getFeatureSource(SystemTestData.BASIC_POLYGONS);
        fct.getFeature().add(fs.getFeatures());

        validateGetFeature(fct, true);

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
        Assert.assertEquals(GeoPkg.MIME_TYPE, resp.getContentType());

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

    public void validateGetFeature(FeatureCollectionResponse fct, boolean indexed)
            throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        format.write(fct, os, op);

        try (GeoPackage geopkg = createGeoPackage(os.toByteArray())) {

            // compare all feature collections
            for (FeatureCollection collection : fct.getFeatures()) {
                FeatureEntry e = new FeatureEntry();
                e.setTableName(collection.getSchema().getName().getLocalPart());
                e.setGeometryColumn(
                        collection.getSchema().getGeometryDescriptor().getName().getLocalPart());

                try (SimpleFeatureReader reader = geopkg.reader(e, null, null)) {

                    SimpleFeatureCollection sCollection = (SimpleFeatureCollection) collection;

                    // spatial index
                    assertEquals(indexed, geopkg.hasSpatialIndex(e));

                    // compare type
                    SimpleFeatureType type1 = reader.getFeatureType();
                    SimpleFeatureType type2 = sCollection.getSchema();
                    assertEquals(type1.getDescriptors().size(), type2.getDescriptors().size());
                    for (int i = 0; i < type1.getDescriptors().size(); i++) {
                        assertEquals(
                                type1.getDescriptor(i).getName(), type2.getDescriptor(i).getName());
                        assertEquals(
                                type1.getDescriptor(i).getType(), type2.getDescriptor(i).getType());
                    }

                    // compare data
                    MemoryFeatureCollection memCollection = new MemoryFeatureCollection(type2);
                    while (reader.hasNext()) {
                        memCollection.add(reader.next());
                    }

                    assertEquals(sCollection.size(), memCollection.size());

                    try (SimpleFeatureIterator it = sCollection.features()) {
                        while (it.hasNext()) {
                            SimpleFeature sf = it.next();
                            for (int i = 0; i < type1.getDescriptors().size(); i++) {
                                assertTrue(
                                        findFeatureAttribute(memCollection, i, sf.getAttribute(i)));
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean findFeatureAttribute(
            SimpleFeatureCollection collection, int indexProp, Object value) {
        try (SimpleFeatureIterator it = collection.features()) {
            while (it.hasNext()) {
                SimpleFeature sf = it.next();
                if (sf.getAttribute(indexProp).equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected GeoPackage createGeoPackage(byte[] inMemory) throws IOException {

        File f = File.createTempFile("temp", ".gpkg", new File("target"));
        try (FileOutputStream fout = new FileOutputStream(f)) {
            fout.write(inMemory);
            fout.flush();
        }

        return new GeoPackage(f);
    }

    public boolean isStandardTemp(File f, String standardTempDir, String customTempDir) {
        String fileDir = f.getAbsoluteFile().getParent();
        return fileDir.equals(standardTempDir);
    }

    public boolean isCustomTemp(File f, String standardTempDir, String customTempDir) {
        String fileDir = f.getAbsoluteFile().getParent();
        return fileDir.equals(customTempDir);
    }

    /**
     * Tests that the CUSTOM_TEMP_DIR_PROPERTY property is being respected. This creates a temp dir
     * and verifies where the temp file is being created (standard java temp dir or our custom temp
     * dir).
     */
    @Test
    public void testTempDir() throws Exception {
        String standardTempDir = FileUtils.getTempDirectory().getAbsolutePath();
        String customTempDir =
                Files.createTempDirectory("geopkg.testcase").toAbsolutePath().toString();
        try {

            // 1. no property -> standard temp dir
            System.clearProperty(format.CUSTOM_TEMP_DIR_PROPERTY);

            File f = format.createTempFile("prefix", "suffix");
            f.delete();
            assertTrue(isStandardTemp(f, standardTempDir, customTempDir));
            assertFalse(isCustomTemp(f, standardTempDir, customTempDir));

            // 2. blank property ("") -> standard temp dir
            System.setProperty(format.CUSTOM_TEMP_DIR_PROPERTY, "");
            f = format.createTempFile("prefix", "suffix");
            f.delete();
            assertTrue(isStandardTemp(f, standardTempDir, customTempDir));
            assertFalse(isCustomTemp(f, standardTempDir, customTempDir));

            // 3. spaces only property  (" ")-> standard temp dir
            System.setProperty(format.CUSTOM_TEMP_DIR_PROPERTY, " ");
            f.delete();
            f = format.createTempFile("prefix", "suffix");
            assertTrue(isStandardTemp(f, standardTempDir, customTempDir));
            assertFalse(isCustomTemp(f, standardTempDir, customTempDir));

            // 4. create in our custom dir
            System.setProperty(format.CUSTOM_TEMP_DIR_PROPERTY, customTempDir);
            f = format.createTempFile("prefix", "suffix");
            f.delete();
            assertFalse(isStandardTemp(f, standardTempDir, customTempDir));
            assertTrue(isCustomTemp(f, standardTempDir, customTempDir));

            // 5. invalid custom path
            System.setProperty(format.CUSTOM_TEMP_DIR_PROPERTY, "BADPATH.BADPATH");
            assertThrows(IOException.class, () -> format.createTempFile("prefix", "suffix"));

        } finally {
            Files.delete((new File(customTempDir)).toPath());
            System.clearProperty(format.CUSTOM_TEMP_DIR_PROPERTY);
        }
    }

    /**
     * GEOS-11015: Check tmp file not orphaned when write operation interrupted due to closed
     * outputstream
     */
    @Test
    public void testTempFileCleanup() throws IOException {
        final FeatureSource<? extends FeatureType, ? extends Feature> basicPolygons =
                getFeatureSource(SystemTestData.BASIC_POLYGONS);

        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(basicPolygons.getFeatures());

        int initialTempFileCount = tempGeoPackageFileCount();
        try {
            // writing to null output stream to simulate stream being closed
            format.write(fct, null, op);
        } catch (NullPointerException expected) {
            // expected failure to similar stream being closed mid operation above
        } finally {
            // check if we leaked any geopkg-*.geopkg java
            int orphaned = tempGeoPackageFileCount() - initialTempFileCount;
            assertTrue("Orphaned java.io.tmpdir tmp.gpkg files: " + orphaned, orphaned <= 0);
        }
    }

    private int tempGeoPackageFileCount() {
        File tmp = new File(System.getProperty("java.io.tmpdir"));
        File files[] =
                tmp.listFiles(
                        pathname ->
                                pathname.getName().startsWith("geopkg")
                                        && pathname.getName().endsWith(".tmp.gpkg"));
        return files.length;
    }
}
