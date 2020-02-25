/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class CatalogBuilderIntTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testLargeNDMosaic() throws Exception {
        // build a mosaic with 1025 files (the standard ulimit is 1024)
        File mosaic = new File("./target/largeMosaic");
        try {
            createTimeMosaic(mosaic, 1025);

            // now configure a new store based on it
            Catalog cat = getCatalog();
            CatalogBuilder cb = new CatalogBuilder(cat);
            CoverageStoreInfo store = cb.buildCoverageStore("largeMosaic");
            store.setURL(mosaic.getAbsolutePath());
            store.setType("ImageMosaic");
            cat.add(store);

            // and configure also the coverage
            cb.setStore(store);
            CoverageInfo ci = cb.buildCoverage();
            cat.add(ci);
            cat.getResourcePool().dispose();
        } finally {
            if (mosaic.exists() && mosaic.isDirectory()) {
                FileUtils.deleteDirectory(mosaic);
            }
        }
    }

    @Test
    public void testMosaicParameters() throws Exception {
        // build a mosaic with 1025 files (the standard ulimit is 1024)
        File mosaic = new File("./target/smallMosaic");
        try {
            createTimeMosaic(mosaic, 4);

            // now configure a new store based on it
            Catalog cat = getCatalog();
            CatalogBuilder cb = new CatalogBuilder(cat);
            CoverageStoreInfo store = cb.buildCoverageStore("smallMosaic");
            store.setURL(mosaic.getAbsolutePath());
            store.setType("ImageMosaic");
            cat.add(store);

            // and configure also the coverage
            cb.setStore(store);
            CoverageInfo ci = cb.buildCoverage();
            cat.add(ci);

            // check the parameters have the default values
            assertEquals(
                    String.valueOf(-1),
                    ci.getParameters()
                            .get(ImageMosaicFormat.MAX_ALLOWED_TILES.getName().toString()));
            assertEquals("", ci.getParameters().get(ImageMosaicFormat.FILTER.getName().toString()));
            cat.getResourcePool().dispose();
        } finally {
            if (mosaic.exists() && mosaic.isDirectory()) {
                FileUtils.deleteDirectory(mosaic);
            }
        }
    }

    private void createTimeMosaic(File mosaic, int fileCount) throws Exception {
        if (mosaic.exists()) {
            if (mosaic.isDirectory()) {
                FileUtils.deleteDirectory(mosaic);
            } else {
                mosaic.delete();
            }
        }
        mosaic.mkdir();
        // System.out.println(mosaic.getAbsolutePath());

        // build the reference coverage into a byte array
        GridCoverageFactory factory = new GridCoverageFactory();
        BufferedImage bi = new BufferedImage(10, 10, BufferedImage.TYPE_4BYTE_ABGR);
        ReferencedEnvelope envelope = new ReferencedEnvelope(0, 10, 0, 10, CRS.decode("EPSG:4326"));
        GridCoverage2D test = factory.create("test", bi, envelope);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GeoTiffWriter writer = new GeoTiffWriter(bos);
        writer.write(test, null);
        writer.dispose();

        // create the lot of files
        byte[] bytes = bos.toByteArray();
        for (int i = 0; i < fileCount; i++) {
            String pad = "";
            if (i < 10) {
                pad = "000";
            } else if (i < 100) {
                pad = "00";
            } else if (i < 1000) {
                pad = "0";
            }
            File target = new File(mosaic, "tile_" + pad + i + ".tiff");
            FileUtils.writeByteArrayToFile(target, bytes);
        }

        // create the mosaic indexer property file
        Properties p = new Properties();
        p.put("ElevationAttribute", "elevation");
        p.put("Schema", "*the_geom:Polygon,location:String,elevation:Integer");
        p.put("PropertyCollectors", "IntegerFileNameExtractorSPI[elevationregex](elevation)");
        FileOutputStream fos = new FileOutputStream(new File(mosaic, "indexer.properties"));
        p.store(fos, null);
        fos.close();
        // and the regex itself
        p.clear();
        p.put("regex", "(?<=_)(\\d{4})");
        fos = new FileOutputStream(new File(mosaic, "elevationregex.properties"));
        p.store(fos, null);
        fos.close();
    }
}
