/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.util.LookupService;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;

public class AppSchemaLocalPublicationTaskTest extends AbstractTaskManagerTest {

    // configure these constants
    private static final String VECTOR_LOCATION = "appschema/MappedFeature2.xml";
    private static final String VECTOR_LOCATION_PUB = "appschema/local/MappedFeature2.xml";
    private static final String ZIP_LOCATION = "appschema/GeologicUnit.zip";
    private static final String ZIP_LOCATION_PUB = "appschema/local/GeologicUnit.xml";
    private static final String ZIP_LOCATION_PUB_2 = "appschema/local/MappedFeature.xml";
    private static final String FILE_SERVICE = "data-directory";
    private static final String VECTOR_WS = "gsml";
    private static final String VECTOR_NAME = "MappedFeature";
    private static final String ZIP_NAME = "GeologicUnit";
    private static final String DB_NAME = "myjndidb";
    private static final String TABLE_NAME = "MappedFeature";

    // attributes
    private static final String ATT_FILE_SERVICE = "fileService";
    private static final String ATT_FILE = "file";
    private static final String ATT_WORKSPACE = "workspace";
    private static final String ATT_LAYER = "layer";
    private static final String ATT_DATABASE = "database";
    private static final String ATT_TABLE_NAME = "table-name";
    private static final String ATT_FAIL = "fail";

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    @Autowired private Catalog catalog;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private LookupService<FileService> fileServices;

    private Configuration config;

    private Batch batch;

    @Override
    public boolean setupDataDirectory() throws Exception {
        return true;
    }

    @Before
    public void setupBatch() throws IOException {
        // copy file if not exists
        FileService fileService = fileServices.get(FILE_SERVICE);
        if (!fileService.checkFileExists(VECTOR_LOCATION)) {
            try (InputStream in =
                    getClass().getResource("appschema/MappedFeature2.xml").openStream()) {
                fileService.create(VECTOR_LOCATION, in);
            }
        }
        if (!fileService.checkFileExists("appschema/local/MappedFeature.properties")) {
            try (InputStream in =
                    getClass().getResource("appschema/MappedFeature.properties").openStream()) {
                fileService.create("appschema/local/MappedFeature.properties", in);
            }
        }
        if (!fileService.checkFileExists("appschema/GeologicUnit.zip")) {
            try (InputStream in =
                    getClass().getResource("appschema/GeologicUnit.zip").openStream()) {
                fileService.create("appschema/GeologicUnit.zip", in);
            }
        }
        // add gsml namespace
        if (catalog.getWorkspaceByName("gsml") == null) {
            CatalogFactory factory = catalog.getFactory();
            NamespaceInfo ns = factory.createNamespace();
            ns.setPrefix("gsml");
            ns.setURI("urn:cgi:xmlns:CGI:GeoSciML:2.0");
            catalog.add(ns);
            WorkspaceInfo ws = factory.createWorkspace();
            ws.setName(ns.getName());
            catalog.add(ws);
        }

        // create configuration
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(AppSchemaLocalPublicationTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, AppSchemaLocalPublicationTaskTypeImpl.PARAM_FILE_SERVICE, ATT_FILE_SERVICE);
        dataUtil.setTaskParameterToAttribute(
                task1, AppSchemaLocalPublicationTaskTypeImpl.PARAM_FILE, ATT_FILE);
        dataUtil.setTaskParameterToAttribute(
                task1, AppSchemaLocalPublicationTaskTypeImpl.PARAM_WORKSPACE, ATT_WORKSPACE);
        dataUtil.setTaskParameterToAttribute(
                task1, AppSchemaLocalPublicationTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.setTaskParameterToAttribute(
                task1, AppSchemaLocalPublicationTaskTypeImpl.PARAM_DB, ATT_DATABASE);
        dataUtil.addTaskToConfiguration(config, task1);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");

        batch = fac.createBatch();

        batch.setName("my_batch");
        dataUtil.addBatchElement(batch, task1);

        batch = bjService.saveAndSchedule(batch);

        config = dao.init(config);
        task1 = config.getTasks().get("task1");
    }

    @After
    public void clearDataFromDatabase() {
        dao.delete(batch);
        dao.delete(config);
    }

    @Test
    public void testSuccessAndCleanup() throws SchedulerException, IOException {
        dataUtil.setConfigurationAttribute(config, ATT_FILE_SERVICE, FILE_SERVICE);
        dataUtil.setConfigurationAttribute(config, ATT_FILE, VECTOR_LOCATION);
        dataUtil.setConfigurationAttribute(config, ATT_WORKSPACE, VECTOR_WS);
        dataUtil.setConfigurationAttribute(config, ATT_LAYER, VECTOR_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_DATABASE, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertNotNull(catalog.getLayerByName(VECTOR_NAME));
        DataStoreInfo csi = catalog.getStoreByName(VECTOR_WS, VECTOR_NAME, DataStoreInfo.class);
        assertNotNull(csi);
        assertEquals(
                "file:data/appschema/local/MappedFeature2.xml",
                csi.getConnectionParameters().get("url").toString());
        assertNotNull(catalog.getResourceByName(VECTOR_NAME, FeatureTypeInfo.class));

        FileService fileService = fileServices.get(FILE_SERVICE);
        assertTrue(fileService.checkFileExists(VECTOR_LOCATION_PUB));
        try (InputStream is = fileService.read(VECTOR_LOCATION_PUB)) {
            String pubStr = IOUtils.toString(is, "UTF-8");
            assertTrue(pubStr.indexOf("<value>myjndi</value>") > 0);
            assertTrue(pubStr.indexOf("<value>gw_beleid</value>") > 0);
            assertTrue(pubStr.indexOf("<sourceType>MappedFeature</sourceType>") > 0);
        }

        taskUtil.cleanup(config);

        assertNull(catalog.getLayerByName(VECTOR_NAME));
        assertNull(catalog.getStoreByName(VECTOR_WS, VECTOR_NAME, DataStoreInfo.class));
        assertNull(catalog.getResourceByName(VECTOR_NAME, FeatureTypeInfo.class));
    }

    @Test
    public void testZipSuccessAndCleanup() throws SchedulerException, IOException {
        dataUtil.setConfigurationAttribute(config, ATT_FILE_SERVICE, FILE_SERVICE);
        dataUtil.setConfigurationAttribute(config, ATT_FILE, ZIP_LOCATION);
        dataUtil.setConfigurationAttribute(config, ATT_WORKSPACE, VECTOR_WS);
        dataUtil.setConfigurationAttribute(config, ATT_LAYER, ZIP_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_DATABASE, DB_NAME);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertNotNull(catalog.getLayerByName(ZIP_NAME));
        DataStoreInfo csi = catalog.getStoreByName(VECTOR_WS, ZIP_NAME, DataStoreInfo.class);
        assertNotNull(csi);
        assertEquals(
                "file:data/appschema/local/GeologicUnit.xml",
                csi.getConnectionParameters().get("url").toString());
        assertNotNull(catalog.getResourceByName(ZIP_NAME, FeatureTypeInfo.class));

        FileService fileService = fileServices.get(FILE_SERVICE);
        assertTrue(fileService.checkFileExists(ZIP_LOCATION_PUB));
        try (InputStream is = fileService.read(ZIP_LOCATION_PUB)) {
            String pubStr = IOUtils.toString(is, "UTF-8");
            assertTrue(pubStr.indexOf("<value>myjndi</value>") > 0);
            assertTrue(pubStr.indexOf("<value>gw_beleid</value>") > 0);
            assertTrue(pubStr.indexOf("<sourceType>GeologicUnit</sourceType>") > 0);
        }
        assertTrue(fileService.checkFileExists(ZIP_LOCATION_PUB_2));
        try (InputStream is = fileService.read(ZIP_LOCATION_PUB_2)) {
            String pubStr = IOUtils.toString(is, "UTF-8");
            assertTrue(pubStr.indexOf("<value>myjndi</value>") > 0);
            assertTrue(pubStr.indexOf("<value>gw_beleid</value>") > 0);
            assertTrue(pubStr.indexOf("<sourceType>MappedFeature</sourceType>") > 0);
        }

        taskUtil.cleanup(config);

        assertNull(catalog.getLayerByName(ZIP_NAME));
        assertNull(catalog.getStoreByName(VECTOR_WS, ZIP_NAME, DataStoreInfo.class));
        assertNull(catalog.getResourceByName(ZIP_NAME, FeatureTypeInfo.class));
    }

    @Test
    public void testRollback() throws SchedulerException {
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task2, TestTaskTypeImpl.PARAM_FAIL, ATT_FAIL);
        dataUtil.addTaskToConfiguration(config, task2);

        dataUtil.setConfigurationAttribute(config, ATT_FILE_SERVICE, FILE_SERVICE);
        dataUtil.setConfigurationAttribute(config, ATT_FILE, VECTOR_LOCATION);
        dataUtil.setConfigurationAttribute(config, ATT_WORKSPACE, VECTOR_WS);
        dataUtil.setConfigurationAttribute(config, ATT_LAYER, VECTOR_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_DATABASE, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_FAIL, Boolean.TRUE.toString());
        config = dao.save(config);
        task2 = config.getTasks().get("task2");
        dataUtil.addBatchElement(batch, task2);
        batch = bjService.saveAndSchedule(batch);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertNull(catalog.getLayerByName(VECTOR_NAME));
        assertNull(catalog.getStoreByName(VECTOR_WS, VECTOR_NAME, DataStoreInfo.class));
        assertNull(catalog.getResourceByName(VECTOR_NAME, FeatureTypeInfo.class));
    }
}
