package org.geoserver.jms.test.rest;
/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
//
//package org.geoserver.cluster.test.rest;
//
//import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;
//import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
//import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
//import it.geosolutions.geoserver.rest.encoder.coverage.GSImageMosaicEncoder;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//
//import org.apache.log4j.Logger;
//import org.junit.Test;
//import org.springframework.core.io.ClassPathResource;
//
///**
// * Testcase for publishing layers on geoserver. We need a running GeoServer to
// * properly run the tests. If such geoserver instance cannot be contacted, tests
// * will be skipped.
// * 
// * @author etj
// * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
// */
//public class GeoserverRESTPublisherTest extends GeoserverRESTTest {
//
//    private final static Logger LOGGER = Logger.getLogger(GeoserverRESTPublisherTest.class);
//
//    public GeoserverRESTPublisherTest() {
//        super("GeoserverRESTPublisherTest");
//    }
//
//    @Test
//    public void testWorkspaces() {
//        if (!enabled())
//            return;
//        deleteAll();
//
//        assertEquals(0, masterReader.getWorkspaces().size());
//
//        assertTrue(publisher.createWorkspace("WS1"));
//        assertTrue(publisher.createWorkspace("WS2"));
//        assertEquals(2, masterReader.getWorkspaces().size());
//
//        try {
//            Thread.sleep(TIMEOUT);
//        } catch (InterruptedException e) {
//            fail(e.getLocalizedMessage());
//        }
//        for (int i = 0; i < nSlaves; i++) {
//            assertEquals(2, reader[i].getWorkspaces().size());
//        }
//    }
//
//    final static String styleName = "restteststyle";
//
//    @Test
//    public void testExternalGeotiff() throws FileNotFoundException, IOException {
//        if (!enabled())
//            return;
//        deleteAll();
//
//        assertEquals(0, masterReader.getStyles().size());
//
//        File sldFile = new ClassPathResource("testdata/restteststyle.sld").getFile();
//
//        // insert style
//        assertTrue(publisher.publishStyle(sldFile));
//        assertTrue(masterReader.existsStyle(styleName));
//
//        try {
//            Thread.sleep(TIMEOUT);
//        } catch (InterruptedException e) {
//            fail(e.getLocalizedMessage());
//        }
//        for (int i = 0; i < nSlaves; i++) {
//            assertTrue(reader[i].existsStyle(styleName));
//        }
//
//        String storeName = "testRESTStoreGeotiff";
//        String layerName = "resttestdem";
//
//        if (masterReader.getWorkspaces().isEmpty())
//            assertTrue(publisher.createWorkspace(DEFAULT_WS));
//
//        File geotiff = new File("/media/share/testdata/resttestdem.tif");
//
//        // known state?
//        // assertFalse("Cleanup failed", existsLayer(layerName));
//
//        // test insert
//        assertTrue("publish() failed", publisher.publishGeoTIFF(DEFAULT_WS, storeName, storeName, geotiff,
//                                                                "EPSG:4326", ProjectionPolicy.FORCE_DECLARED,
//                                                                styleName));
//
//        assertTrue(existsLayer(layerName));
//
//        RESTCoverageStore reloadedCS = masterReader.getCoverageStore(DEFAULT_WS, storeName);
//        assertNotNull(reloadedCS);
//        try {
//            Thread.sleep(TIMEOUT);
//        } catch (InterruptedException e) {
//            fail(e.getLocalizedMessage());
//        }
//        for (int i = 0; i < nSlaves; i++) {
//            RESTCoverageStore slaveCS = reader[i].getCoverageStore(DEFAULT_WS, storeName);
//            assertNotNull("Unable to get coverageStore for reader n.:" + i, slaveCS);
//            // assertTrue(reloadedCS.equals(slaveCS));
//        }
//
//    }
//
//    @Test
//    public void testCreateDeleteImageMosaicDatastore() {
//        if (!enabled()) {
//            return;
//        }
//        deleteAll();
//
//        final String coverageStoreName = "resttestImageMosaic";
//
//        final GSImageMosaicEncoder coverageEncoder = new GSImageMosaicEncoder();
//
//        coverageEncoder.setAllowMultithreading(true);
//        coverageEncoder.setBackgroundValues("");
//        coverageEncoder.setFilter("");
//        coverageEncoder.setInputTransparentColor("");
//        coverageEncoder.setLatLonBoundingBox(-180, -90, 180, 90, "EPSG:4326");
//        coverageEncoder.setMaxAllowedTiles(6000);
//        coverageEncoder.setNativeBoundingBox(-180, -90, 180, 90, "EPSG:4326");
//        coverageEncoder.setOutputTransparentColor("");
//        coverageEncoder.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
//        coverageEncoder.setSRS("EPSG:4326");
//        coverageEncoder.setSUGGESTED_TILE_SIZE("256,256");
//        coverageEncoder.setUSE_JAI_IMAGEREAD(true);
//        // activate time
//        // final GSDimensionInfoEncoder time=new GSDimensionInfoEncoder(true);
//        // time.setPresentation(Presentation.LIST);
//        // // set time metadata
//        // coverageEncoder.setMetadata("time", time);
//        // // not active elevation
//        // coverageEncoder.setMetadata("elevation", new
//        // GSDimensionInfoEncoder());
//
//        if (masterReader.getWorkspaces().isEmpty())
//            assertTrue(publisher.createWorkspace(DEFAULT_WS));
//
//        LOGGER.info(coverageEncoder.toString());
//
//        // final String styleName = "testRasterStyle3";
//        // File sldFile;
//        // try {
//        // sldFile = new ClassPathResource("testdata/raster.sld").getFile();
//        // // insert style
//        // assertTrue(publisher.publishStyle(sldFile,styleName));
//        // } catch (IOException e1) {
//        // assertFalse(e1.getLocalizedMessage(),Boolean.FALSE);
//        // e1.printStackTrace();
//        // }
//
//        GSLayerEncoder layerEncoder = new GSLayerEncoder();
//
//        layerEncoder.setDefaultStyle(styleName);
//        LOGGER.info(layerEncoder.toString());
//        // creation test
//        RESTCoverageStore coverageStore = null;
//        try {
//            final File mosaicFile = new File("/media/share/time_geotiff/");
//
//            if (!publisher.createExternalMosaic(DEFAULT_WS, coverageStoreName, mosaicFile, coverageEncoder,
//                                                layerEncoder)) {
//                fail();
//            }
//            coverageStore = masterReader.getCoverageStore(DEFAULT_WS, coverageStoreName);
//
//            if (coverageStore == null) {
//                LOGGER.error("*** coveragestore " + coverageStoreName + " has not been created.");
//                fail("*** coveragestore " + coverageStoreName + " has not been created.");
//            }
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            fail(e.getLocalizedMessage());
//        } catch (IOException e) {
//            e.printStackTrace();
//            fail(e.getLocalizedMessage());
//        }
//
//        // removing recursively coveragestore
//        boolean removed = publisher.removeCoverageStore(coverageStore.getWorkspaceName(),
//                                                        coverageStore.getName(), true);
//        if (!removed) {
//            LOGGER.error("*** CoverageStore " + coverageStoreName + " has not been removed.");
//            fail("*** CoverageStore " + coverageStoreName + " has not been removed.");
//        }
//
//    }
//
//    private boolean existsLayer(String layername) {
//        try {
//            Thread.sleep(TIMEOUT);
//        } catch (InterruptedException e) {
//            fail(e.getLocalizedMessage());
//        }
//        boolean test = masterReader.getLayer(layername) != null;
//        if (!test) {
//            LOGGER.info("Layer is not present on the master");
//            return test;
//        }
//        for (int i = 0; i < nSlaves; i++) {
//            test = test && (reader[i].getLayer(layername) != null);
//            if (!test) {
//                LOGGER.info("Layer is not present on slave n: " + i + " toString: " + reader[i].toString());
//            }
//        }
//        return test;
//    }
//}
