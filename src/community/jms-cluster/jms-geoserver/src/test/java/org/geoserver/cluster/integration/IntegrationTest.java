/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.integration;

import static org.geoserver.cluster.integration.IntegrationTestsUtils.checkNoDifferences;
import static org.geoserver.cluster.integration.IntegrationTestsUtils.differences;
import static org.geoserver.cluster.integration.IntegrationTestsUtils.equalizeInstances;
import static org.geoserver.cluster.integration.IntegrationTestsUtils.resetEventsCount;
import static org.geoserver.cluster.integration.IntegrationTestsUtils.resetJmsConfiguration;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.IOUtils;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for JMS that tests that GeoServer configurations events and GeoServer catalog
 * events are correctly propagated and handled.
 */
public final class IntegrationTest {

    // instantiate some GeoServer instances

    private static final GeoServerInstance INSTANCE_A = new GeoServerInstance("INSTANCE-A");
    private static final GeoServerInstance INSTANCE_B = new GeoServerInstance("INSTANCE-B");
    private static final GeoServerInstance INSTANCE_C = new GeoServerInstance("INSTANCE-C");
    private static final GeoServerInstance INSTANCE_D = new GeoServerInstance("INSTANCE-D");

    private static final GeoServerInstance[] INSTANCES =
            new GeoServerInstance[] {INSTANCE_A, INSTANCE_B, INSTANCE_C, INSTANCE_D};

    @Before
    public void resetInstances() {
        // disable JMS before equalizing the instances configuration and catalog
        Arrays.stream(INSTANCES)
                .forEach(
                        instance -> {
                            instance.disableJmsMaster();
                            instance.disableJmsSlave();
                        });
        // equalize the configuration and the catalog
        equalizeInstances(INSTANCES);
        // reset JMS configuration and events count
        resetJmsConfiguration(INSTANCES);
        resetEventsCount(INSTANCES);
    }

    @AfterClass
    public static void tearDown() {
        // destroy all instances
        Arrays.stream(INSTANCES).forEach(GeoServerInstance::destroy);
    }

    @Test
    public void testConfigurationMastersSlavesApplyToMaster() throws Exception {
        // assert instances are equal
        checkNoDifferences(INSTANCES);
        // use instance A for control
        INSTANCE_A.disableJmsMaster();
        INSTANCE_A.disableJmsSlave();
        // instance B will be a pure master and instances C and D pure slaves
        INSTANCE_B.disableJmsSlave();
        INSTANCE_C.disableJmsMaster();
        INSTANCE_D.disableJmsMaster();
        // apply add and modify configuration changes to master
        applyAddModifyConfigurationChanges(INSTANCE_B);
        // check instance C
        waitAndCheckEvents(INSTANCE_C, 9);
        List<InfoDiff> differences = differences(INSTANCE_B, INSTANCE_C);
        assertThat(differences.size(), is(0));
        // check instance D
        waitAndCheckEvents(INSTANCE_D, 9);
        differences = differences(INSTANCE_B, INSTANCE_D);
        assertThat(differences.size(), is(0));
        // check instance A
        waitAndCheckEvents(INSTANCE_A, 0);
        differences = differences(INSTANCE_B, INSTANCE_A);
        assertThat(differences.size(), is(4));
        // apply remove configuration changes to master
        applyDeleteConfigurationChanges(INSTANCE_B);
        // check instance C
        waitAndCheckEvents(INSTANCE_C, 4);
        differences = differences(INSTANCE_B, INSTANCE_C);
        assertThat(differences.size(), is(0));
        // check instance D
        waitAndCheckEvents(INSTANCE_D, 4);
        differences = differences(INSTANCE_B, INSTANCE_D);
        assertThat(differences.size(), is(0));
        // check instance A
        waitAndCheckEvents(INSTANCE_A, 0);
        differences = differences(INSTANCE_B, INSTANCE_A);
        assertThat(differences.size(), is(2));
    }

    @Test
    public void testConfigurationMastersSlavesApplyToSlave() throws Exception {
        // assert instances are equal
        checkNoDifferences(INSTANCES);
        // use instance A for control
        INSTANCE_A.disableJmsMaster();
        INSTANCE_A.disableJmsSlave();
        // instance B will be a pure master and instances C and D pure slaves
        INSTANCE_B.disableJmsSlave();
        INSTANCE_C.disableJmsMaster();
        INSTANCE_D.disableJmsMaster();
        // apply add and modify configuration changes to slave
        applyAddModifyConfigurationChanges(INSTANCE_C);
        // check instance A
        waitNoEvents(INSTANCE_A, 100);
        List<InfoDiff> differences = differences(INSTANCE_C, INSTANCE_A);
        assertThat(differences.size(), is(4));
        // check instance B
        waitNoEvents(INSTANCE_B, 100);
        differences = differences(INSTANCE_C, INSTANCE_B);
        assertThat(differences.size(), is(4));
        // check instance D
        waitNoEvents(INSTANCE_D, 100);
        differences = differences(INSTANCE_C, INSTANCE_D);
        assertThat(differences.size(), is(4));
        // apply remove configuration changes to slave
        applyDeleteConfigurationChanges(INSTANCE_C);
        // check instance A
        waitNoEvents(INSTANCE_A, 100);
        differences = differences(INSTANCE_C, INSTANCE_A);
        assertThat(differences.size(), is(2));
        // check instance B
        waitNoEvents(INSTANCE_C, 100);
        differences = differences(INSTANCE_C, INSTANCE_B);
        assertThat(differences.size(), is(2));
        // check instance D
        waitNoEvents(INSTANCE_D, 100);
        differences = differences(INSTANCE_C, INSTANCE_D);
        assertThat(differences.size(), is(2));
    }

    @Test
    public void testCatalogMastersSlavesApplyToMaster() throws Exception {
        // assert instances are equal
        checkNoDifferences(INSTANCES);
        // use instance A for control
        INSTANCE_A.disableJmsMaster();
        INSTANCE_A.disableJmsSlave();
        // instance B will be a pure master and instances C and D pure slaves
        INSTANCE_B.disableJmsSlave();
        INSTANCE_C.disableJmsMaster();
        INSTANCE_D.disableJmsMaster();
        // apply catalog add changes to master
        applyAddCatalogChanges(INSTANCE_B);
        // check instance C
        waitAndCheckEvents(INSTANCE_C, 25);
        List<InfoDiff> differences = differences(INSTANCE_B, INSTANCE_C);
        assertThat(differences.size(), is(0));
        // check instance D
        waitAndCheckEvents(INSTANCE_D, 25);
        differences = differences(INSTANCE_B, INSTANCE_D);
        assertThat(differences.size(), is(0));
        // check instance A
        waitAndCheckEvents(INSTANCE_A, 0);
        differences = differences(INSTANCE_B, INSTANCE_A);
        assertThat(differences.size(), is(11));
        // apply modify changes to the catalog
        applyModifyCatalogChanges(INSTANCE_B);
        // check instance C
        waitAndCheckEvents(INSTANCE_C, 20);
        differences = differences(INSTANCE_B, INSTANCE_C);
        assertThat(differences.size(), is(0));
        // check instance D
        waitAndCheckEvents(INSTANCE_D, 20);
        differences = differences(INSTANCE_B, INSTANCE_D);
        assertThat(differences.size(), is(0));
        // check instance A
        waitAndCheckEvents(INSTANCE_A, 0);
        differences = differences(INSTANCE_B, INSTANCE_A);
        assertThat(differences.size(), is(11));
        // apply catalog delete events
        applyDeleteCatalogChanges(INSTANCE_B);
        // check instance C
        waitAndCheckEvents(INSTANCE_C, 28);
        differences = differences(INSTANCE_B, INSTANCE_C);
        assertThat(differences.size(), is(0));
        // check instance D
        waitAndCheckEvents(INSTANCE_D, 28);
        differences = differences(INSTANCE_B, INSTANCE_D);
        assertThat(differences.size(), is(0));
        // check instance A
        waitAndCheckEvents(INSTANCE_A, 0);
        differences = differences(INSTANCE_B, INSTANCE_A);
        assertThat(differences.size(), is(0));
    }

    /**
     * Helper methods that waits a specified amount of time and checks that no events were consumed.
     */
    private void waitNoEvents(GeoServerInstance instance, int waitTimeMs) {
        try {
            // wait the specified amount of time
            Thread.sleep(waitTimeMs);
        } catch (InterruptedException exception) {
            // well we got interrupted
            Thread.currentThread().interrupt();
        }
        // check that no events were consumed
        assertThat(instance.getConsumedEventsCount(), is(0));
    }

    /**
     * Waits for the expected number of events to be consumed or for the timeout of two seconds to
     * be reached and then checks if the expected number of events were consumed.
     */
    private void waitAndCheckEvents(GeoServerInstance instance, int expectedEvents) {
        instance.waitEvents(expectedEvents, 2000);
        assertThat(instance.getConsumedEventsCount(), is(expectedEvents));
        instance.resetConsumedEventsCount();
    }

    /**
     * Helper method that adds some new services and settings to the provided GeoServer instance and
     * also modifies some existing ones.
     */
    private void applyAddModifyConfigurationChanges(GeoServerInstance instance) {
        GeoServer geoServer = instance.getGeoServer();
        Catalog catalog = instance.getCatalog();
        WorkspaceInfo workspace = catalog.getWorkspaceByName(MockData.DEFAULT_PREFIX);
        // change GeoServer global settings
        GeoServerInfo geoServerInfo = geoServer.getGlobal();
        SettingsInfo geoServerSettings = geoServerInfo.getSettings();
        ContactInfo geoServerContact = geoServerSettings.getContact();
        geoServerContact.setContactPerson(randomString());
        geoServerSettings.setContact(geoServerContact);
        geoServerInfo.setSettings(geoServerSettings);
        geoServer.save(geoServerInfo);
        // create workspace specific settings
        assertThat(workspace, notNullValue());
        SettingsInfo workspaceSettings = new SettingsInfoImpl();
        workspaceSettings.setTitle(randomString());
        workspaceSettings.setWorkspace(workspace);
        geoServer.add(workspaceSettings);
        // change WMS service settings
        ServiceInfo wmsService = geoServer.getService(WMSInfo.class);
        wmsService.setAbstract(randomString());
        geoServer.save(wmsService);
        // create workspace specific settings for WMS service
        WMSInfoImpl workspaceWmsService = new WMSInfoImpl();
        workspaceWmsService.setName(randomString());
        workspaceWmsService.setTitle(randomString());
        workspaceWmsService.setWorkspace(workspace);
        geoServer.add(workspaceWmsService);
    }

    /**
     * Helper method that removes some services and settings from the provided GeoServer instance.
     */
    private void applyDeleteConfigurationChanges(GeoServerInstance instance) {
        GeoServer geoServer = instance.getGeoServer();
        Catalog catalog = instance.getCatalog();
        WorkspaceInfo workspace = catalog.getWorkspaceByName(MockData.DEFAULT_PREFIX);
        // remove workspace specific settings
        geoServer.remove(geoServer.getSettings(workspace));
        // remove WMS workspace specific settings
        geoServer.remove(geoServer.getService(workspace, WMSInfo.class));
    }

    /** Helper method that add some new catalog elements to the provided GeoServer instance. */
    private void applyAddCatalogChanges(GeoServerInstance instance) {
        // instantiate some common objects
        Catalog catalog = instance.getCatalog();
        ReferencedEnvelope envelope =
                new ReferencedEnvelope(-1.0, 1.0, -2.0, 2.0, DefaultGeographicCRS.WGS84);
        AttributionInfo attribution = new AttributionInfoImpl();
        attribution.setTitle("attribution-Title");
        attribution.setHref("attribution-Href");
        attribution.setLogoURL("attribution-LogoURL");
        attribution.setLogoType("attribution-LogoType");
        attribution.setLogoWidth(500);
        attribution.setLogoHeight(600);
        // add workspace
        WorkspaceInfo workspace = new WorkspaceInfoImpl();
        workspace.setName("workspace-Name");
        catalog.add(workspace);
        // add namespace
        NamespaceInfo namespace = new NamespaceInfoImpl();
        namespace.setPrefix(workspace.getName());
        namespace.setURI("namespace-URI");
        catalog.add(namespace);
        // add data store
        DataStoreInfo dataStore = new DataStoreInfoImpl(catalog);
        dataStore.setEnabled(false);
        dataStore.setName("dataStore-Name");
        dataStore.setWorkspace(workspace);
        dataStore.setType("dataStore-Type");
        dataStore.setDescription("dataStore-Description");
        catalog.add(dataStore);
        // add coverage store
        CoverageStoreInfo coverageStore = new CoverageStoreInfoImpl(catalog);
        coverageStore.setEnabled(false);
        coverageStore.setName("coverageStore-Name");
        coverageStore.setWorkspace(workspace);
        coverageStore.setType("coverageStore-Type");
        coverageStore.setDescription("coverageStore-Description");
        catalog.add(coverageStore);
        // add WMS store
        WMSStoreInfo wmsStore = new WMSStoreInfoImpl(catalog);
        wmsStore.setEnabled(false);
        wmsStore.setName("wmsStore-Name");
        wmsStore.setWorkspace(workspace);
        wmsStore.setType("wmsStore-Type");
        wmsStore.setDescription("wmsStore-Description");
        wmsStore.setCapabilitiesURL("wmsStore-CapabilitiesURL");
        wmsStore.setUseConnectionPooling(false);
        wmsStore.setUsername("wmsStore-Username");
        wmsStore.setPassword("wmsStore-Password");
        wmsStore.setMaxConnections(0);
        wmsStore.setReadTimeout(0);
        wmsStore.setConnectTimeout(0);
        catalog.add(wmsStore);
        // add feature type
        FeatureTypeInfo featureType = new FeatureTypeInfoImpl(catalog);
        featureType.setName("featureType-Name");
        featureType.setNativeName("featureType-NativeName");
        featureType.setNamespace(namespace);
        featureType.setTitle("featureType-Title");
        featureType.setDescription("featureType-Description");
        featureType.setAbstract("featureType-Abstract");
        featureType.setSRS("EPSG:4326");
        featureType.setLatLonBoundingBox(envelope);
        featureType.setEnabled(false);
        featureType.setStore(dataStore);
        featureType.setNativeBoundingBox(envelope);
        featureType.setNativeCRS(DefaultGeographicCRS.WGS84);
        featureType.setAdvertised(false);
        featureType.setMaxFeatures(100);
        featureType.setNumDecimals(4);
        featureType.setOverridingServiceSRS(false);
        featureType.setSkipNumberMatched(false);
        featureType.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        catalog.add(featureType);
        // add coverage
        CoverageInfo coverage = new CoverageInfoImpl(catalog);
        coverage.setName("coverage-Name");
        coverage.setNativeName("coverage-NativeName");
        coverage.setNamespace(namespace);
        coverage.setAbstract("coverage-Abstract");
        coverage.setDescription("coverage-Description");
        coverage.setLatLonBoundingBox(envelope);
        coverage.setNativeBoundingBox(envelope);
        coverage.setSRS("EPSG:4326");
        coverage.setNativeCRS(DefaultGeographicCRS.WGS84);
        coverage.setEnabled(false);
        coverage.setStore(coverageStore);
        coverage.setAdvertised(false);
        coverage.setNativeCoverageName("coverage-NativeCoverageName");
        coverage.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        catalog.add(coverage);
        // add style info and style file
        copyStyle(instance, "/test_style.sld", "test_style.sld");
        StyleInfo style = new StyleInfoImpl(catalog);
        style.setName("style-Name");
        style.setFormat("sld");
        style.setFilename("test_style.sld");
        catalog.add(style);
        // add layer info
        LayerInfo layer = new LayerInfoImpl();
        layer.setResource(featureType);
        layer.setAbstract("layer-Abstract");
        layer.setAttribution(attribution);
        layer.setType(PublishedType.VECTOR);
        layer.setDefaultStyle(style);
        layer.setEnabled(false);
        layer.setQueryable(false);
        layer.setOpaque(false);
        layer.setAdvertised(false);
        catalog.add(layer);
        // add WMS layer info
        WMSLayerInfo wmsLayer = new WMSLayerInfoImpl(catalog);
        wmsLayer.setName("wmsLayer-Name");
        wmsLayer.setNativeName("wmsLayer-NativeName");
        wmsLayer.setNamespace(namespace);
        wmsLayer.setTitle("wmsLayer-Title");
        wmsLayer.setAbstract("wmsLayer-Abstract");
        wmsLayer.setDescription("wmsLayer-Description");
        wmsLayer.setLatLonBoundingBox(envelope);
        wmsLayer.setNativeBoundingBox(envelope);
        wmsLayer.setSRS("EPSG:4326");
        wmsLayer.setNativeCRS(DefaultGeographicCRS.WGS84);
        wmsLayer.setEnabled(false);
        wmsLayer.setStore(wmsStore);
        wmsLayer.setAdvertised(false);
        catalog.add(wmsLayer);
        // layer group
        LayerGroupInfo layerGroup = new LayerGroupInfoImpl();
        layerGroup.setTitle("layerGroup-Title");
        layerGroup.setName("layerGroup-Name");
        layerGroup.setMode(Mode.SINGLE);
        layerGroup.setQueryDisabled(false);
        layerGroup.setBounds(envelope);
        layerGroup.getLayers().add(layer);
        layerGroup.getStyles().add(style);
        catalog.add(layerGroup);
    }

    /** Helper method that apply some catalog changes to the provided GeoServer instance. */
    private void applyModifyCatalogChanges(GeoServerInstance instance) {
        Catalog catalog = instance.getCatalog();
        // change namespace
        NamespaceInfo namespace = catalog.getNamespaceByPrefix("workspace-Name");
        namespace.setURI("namespace-URI-modified");
        catalog.save(namespace);
        // change data store
        DataStoreInfo dataStore = catalog.getDataStoreByName("dataStore-Name");
        dataStore.setDescription("dataStore-Description-modified");
        catalog.save(dataStore);
        // change coverage store
        CoverageStoreInfo coverageStore = catalog.getCoverageStoreByName("coverageStore-Name");
        coverageStore.setDescription("coverageStore-Description-modified");
        catalog.save(coverageStore);
        // change WMS store
        WMSStoreInfo wmsStore = catalog.getStoreByName("wmsStore-Name", WMSStoreInfo.class);
        wmsStore.setDescription("wmsStore-Description-modified");
        catalog.save(wmsStore);
        // change feature type
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName("featureType-Name");
        featureType.setDescription("featureType-Description-modified");
        catalog.save(featureType);
        // change coverage
        CoverageInfo coverage = catalog.getCoverageByName("coverage-Name");
        coverage.setAbstract("coverage-Abstract-modified");
        catalog.save(coverage);
        // change style info
        StyleInfo style = catalog.getStyleByName("style-Name");
        style.setName("style-Name-modified");
        catalog.save(style);
        // change layer info
        LayerInfo layer = catalog.getLayerByName("featureType-Name");
        layer.setAbstract("layer-Abstract-modified");
        catalog.save(layer);
        // change WMS layer info
        WMSLayerInfo wmsLayer = catalog.getResourceByName("wmsLayer-Name", WMSLayerInfo.class);
        wmsLayer.setAbstract("wmsLayer-Abstract-modified");
        catalog.save(wmsLayer);
        // change group
        LayerGroupInfo layerGroup = catalog.getLayerGroupByName("layerGroup-Name");
        layerGroup.setTitle("layerGroup-Title-modified");
        catalog.save(layerGroup);
    }

    /** Helper method that removes some elements from the catalog of the provided instance. */
    private void applyDeleteCatalogChanges(GeoServerInstance instance) {
        Catalog catalog = instance.getCatalog();
        // delete group
        LayerGroupInfo layerGroup = catalog.getLayerGroupByName("layerGroup-Name");
        catalog.remove(layerGroup);
        // delete layer info
        LayerInfo layer = catalog.getLayerByName("featureType-Name");
        catalog.remove(layer);
        // delete WMS layer info
        WMSLayerInfo wmsLayer = catalog.getResourceByName("wmsLayer-Name", WMSLayerInfo.class);
        catalog.remove(wmsLayer);
        // delete feature type
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName("featureType-Name");
        catalog.remove(featureType);
        // delete style info
        StyleInfo style = catalog.getStyleByName("style-Name-modified");
        catalog.remove(style);
        // delete coverage
        CoverageInfo coverage = catalog.getCoverageByName("coverage-Name");
        catalog.remove(coverage);
        // delete data store
        DataStoreInfo dataStore = catalog.getDataStoreByName("dataStore-Name");
        catalog.remove(dataStore);
        // delete coverage store
        CoverageStoreInfo coverageStore = catalog.getCoverageStoreByName("coverageStore-Name");
        catalog.remove(coverageStore);
        // delete WMS store
        WMSStoreInfo wmsStore = catalog.getStoreByName("wmsStore-Name", WMSStoreInfo.class);
        catalog.remove(wmsStore);
        // delete namespace
        NamespaceInfo namespace = catalog.getNamespaceByPrefix("workspace-Name");
        catalog.remove(namespace);
    }

    /** Helper method that copies a style file to the provided GeoServer instance. */
    private void copyStyle(GeoServerInstance instance, String resource, String fileName) {
        Resource styleResource =
                instance.getDataDirectory().get("styles" + File.separator + fileName);
        try (OutputStream output = styleResource.out();
                InputStream input = this.getClass().getResourceAsStream(resource)) {
            IOUtils.copy(input, output);
        } catch (Exception exception) {
            throw new RuntimeException("Error copying test style.", exception);
        }
    }

    /** Helper method that simply returns a random string. */
    public static String randomString() {
        return UUID.randomUUID().toString();
    }
}
