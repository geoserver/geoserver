/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.vectormosaic.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geotools.api.data.DataAccessFactory;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.vectormosaic.VectorMosaicStoreFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class VectorMosaicStoreControllerTest extends CatalogRESTTestSupport {

    private static final String WS_NAME = "vm";
    private static final String NS_URI = "http://example.com/vm";
    private static final String DELEGATE_NAME_SHAPEFILE = "vm_delegate_shp";
    private static final String DELEGATE_NAME_PARQUET = "vm_delegate_prq";
    private static final String VMOSAIC_SHAPE_NAME = "vmosaicshp";
    private static final String VMOSAIC_PRQ_NAME = "vmosaicprq";
    private WorkspaceInfo ws;
    private NamespaceInfo ns;
    private File delegateDir;
    private File vectorMosaicForShapefile;
    private File vectorMosaicForParquet;

    @Before
    public void setUpVectorMosaic() throws Exception {
        ws = catalog.getWorkspaceByName(WS_NAME);
        if (ws == null) {
            ws = catalog.getFactory().createWorkspace();
            ws.setName(WS_NAME);
            catalog.add(ws);
        }

        ns = catalog.getNamespaceByPrefix(WS_NAME);
        if (ns == null) {
            ns = catalog.getFactory().createNamespace();
            ns.setPrefix(WS_NAME);
            ns.setURI(NS_URI);
            catalog.add(ns);
        }
        vectorMosaicForShapefile = setupVectorShapefiles();
        vectorMosaicForParquet = setupVectorParquets();
    }

    private File setupVectorShapefiles() throws Exception {
        Catalog catalog = getCatalog();
        File propertyDatastore = setupDatastore(VectorMosaicStoreControllerTest.DELEGATE_NAME_SHAPEFILE);
        // Create the PropertyDatastore delegate
        DataStoreInfo delegate =
                catalog.getDataStoreByName(WS_NAME, VectorMosaicStoreControllerTest.DELEGATE_NAME_SHAPEFILE);
        if (delegate == null) {
            delegate = catalog.getFactory().createDataStore();
            delegate.setWorkspace(ws);
            delegate.setName(VectorMosaicStoreControllerTest.DELEGATE_NAME_SHAPEFILE);
            delegate.setType(new PropertyDataStoreFactory().getDisplayName());

            Map<String, Serializable> params = delegate.getConnectionParameters();
            params.put(PropertyDataStoreFactory.DIRECTORY.key, propertyDatastore);
            params.put(PropertyDataStoreFactory.NAMESPACE.key, NS_URI);
            catalog.add(delegate);
        }

        // Create the vector mosaic store
        DataStoreInfo vmStore = catalog.getDataStoreByName(WS_NAME, VectorMosaicStoreControllerTest.VMOSAIC_SHAPE_NAME);
        if (vmStore == null) {
            vmStore = catalog.getFactory().createDataStore();
            vmStore.setWorkspace(ws);
            vmStore.setName(VectorMosaicStoreControllerTest.VMOSAIC_SHAPE_NAME);

            DataAccessFactory vmFactory = new VectorMosaicStoreFactory();
            vmStore.setType(vmFactory.getDisplayName());

            Map<String, Serializable> vmParams = vmStore.getConnectionParameters();
            vmParams.put("delegateStoreName", DELEGATE_NAME_SHAPEFILE);
            vmParams.put("connectionParameterKey", "params");
            vmParams.put("preferredDataStoreSPI", "org.geotools.data.shapefile.ShapefileDataStoreFactory");
            vmParams.put("commonParameters", "PropertyCollectors=TimestampFileNameExtractorSPI[[0-9]{8}](time)");
            catalog.add(vmStore);
        }
        return propertyDatastore;
    }

    private File setupVectorParquets() throws Exception {
        Catalog catalog = getCatalog();
        File PropertyDatastore = setupDatastore(DELEGATE_NAME_PARQUET);
        // Create the PropertyDatastore delegate
        DataStoreInfo delegate = catalog.getDataStoreByName(WS_NAME, DELEGATE_NAME_PARQUET);
        if (delegate == null) {
            delegate = catalog.getFactory().createDataStore();
            delegate.setWorkspace(ws);
            delegate.setName(DELEGATE_NAME_PARQUET);
            delegate.setType(new PropertyDataStoreFactory().getDisplayName());
            Map<String, Serializable> params = delegate.getConnectionParameters();
            params.put(PropertyDataStoreFactory.DIRECTORY.key, PropertyDatastore);
            params.put(PropertyDataStoreFactory.NAMESPACE.key, NS_URI);
            catalog.add(delegate);
        }

        // Create the vector mosaic store
        DataStoreInfo vmStore = catalog.getDataStoreByName(WS_NAME, VMOSAIC_PRQ_NAME);
        if (vmStore == null) {
            vmStore = catalog.getFactory().createDataStore();
            vmStore.setWorkspace(ws);
            vmStore.setName(VMOSAIC_PRQ_NAME);
            DataAccessFactory vmFactory = new VectorMosaicStoreFactory();
            vmStore.setType(vmFactory.getDisplayName());
            Map<String, Serializable> vmParams = vmStore.getConnectionParameters();
            vmParams.put("delegateStoreName", DELEGATE_NAME_PARQUET);
            vmParams.put("connectionParameterKey", "params");
            vmParams.put("preferredDataStoreSPI", "org.geotools.dggs.datastore.DGGSStoreFactory");
            vmParams.put(
                    "commonParameters",
                    "dggs_id=H3\\nzoneIdColumnName=h3indexstr\\nresolution=10" + "\\ndelegate.dbtype=geoparquet");
            catalog.add(vmStore);
        }
        return PropertyDatastore;
    }

    private File setupDatastore(String delegate) throws IOException {
        // --- create PropertyDataStore as delegate --------
        GeoServerDataDirectory dataDir = getDataDirectory();
        delegateDir = new File(dataDir.root(), delegate);
        delegateDir.mkdirs();
        String delegateProperties = delegate + ".properties";
        File vectorMosaicDatastore = new File(delegateDir, delegateProperties);
        vectorMosaicDatastore.deleteOnExit();
        try (FileOutputStream fout = new FileOutputStream(vectorMosaicDatastore)) {
            IOUtils.copy(getClass().getResourceAsStream("test-data/" + delegateProperties), fout);
            fout.flush();
        }
        return vectorMosaicDatastore;
    }

    @Test
    public void testShapefileHarvestingInMosaic() throws Exception {
        File dir = new File("./target/empty");
        dir.mkdir();
        dir.deleteOnExit();

        // Creating the coverageStore
        File g1 = new File(dir, "granule1.zip");
        g1.deleteOnExit();
        try (FileOutputStream fout = new FileOutputStream(g1)) {
            IOUtils.copy(getClass().getResourceAsStream("test-data/granule1_20250101.zip"), fout);
            fout.flush();
        }

        final int length = (int) g1.length();
        byte[] zipData = new byte[length];
        try (FileInputStream fis = new FileInputStream(g1)) {
            fis.read(zipData);
        }

        MockHttpServletResponse response = postAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/vm/datastores/" + VMOSAIC_SHAPE_NAME
                        + "/mosaic/file.shp?spi=default",
                zipData,
                "application/zip");
        // Store is created
        assertEquals(202, response.getStatus());
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(vectorMosaicForShapefile)) {
            props.load(in);
        }

        String firstKey = props.stringPropertyNames().iterator().next();
        String firstValue = props.getProperty(firstKey);
        String featureAttributes[] = firstValue.split("\\|");
        assertEquals(
                "POLYGON ((-80.61513697870227 37.06228260413163, -78.02707286292329 37.06228260413163, -78.02707286292329 38.64689485238214, -80.61513697870227 38.64689485238214, -80.61513697870227 37.06228260413163))",
                featureAttributes[0]);
        assertTrue(featureAttributes[1].endsWith("granule1_20250101.shp"));
        assertEquals("2025-01-01T00:00:00Z", featureAttributes[2]);
    }

    @Test
    public void testDGGSParquetHarvesting() throws Exception {
        File dir = new File("./target/empty");
        dir.mkdir();
        dir.deleteOnExit();

        // Creating the coverageStore
        File g1 = new File(dir, "localsample.parquet");
        g1.deleteOnExit();
        try (FileOutputStream fout = new FileOutputStream(g1)) {
            IOUtils.copy(getClass().getResourceAsStream("test-data/localsample.parquet"), fout);
            fout.flush();
        }
        final int length = (int) g1.length();
        byte[] fileData = new byte[length];
        try (FileInputStream fis = new FileInputStream(g1)) {
            fis.read(fileData);
        }

        MockHttpServletResponse response = postAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/vm/datastores/" + VMOSAIC_PRQ_NAME
                        + "/mosaic/file.parquet?spi=dggs",
                fileData,
                "application/octet-stream");

        // Store is created
        assertEquals(202, response.getStatus());
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(vectorMosaicForParquet)) {
            props.load(in);
        }

        String firstKey = props.stringPropertyNames().iterator().next();
        String firstValue = props.getProperty(firstKey);
        String[] featureAttributes = firstValue.split("\\|");
        assertEquals(
                "POLYGON ((8.281649071412575 40.002808662255894, 29.056188263006003 40.002808662255894, 29.056188263006003 56.43981578203419, 8.281649071412575 56.43981578203419, 8.281649071412575 40.002808662255894))",
                featureAttributes[0]);
        assertTrue(featureAttributes[1].contains("vmosaicprq.parquet"));
    }
}
