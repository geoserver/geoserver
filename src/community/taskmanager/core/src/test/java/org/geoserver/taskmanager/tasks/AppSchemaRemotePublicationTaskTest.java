/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.decoder.RESTFeatureType;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.util.LookupService;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.geotools.data.complex.AppSchemaDataAccessFactory;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * To run this test you should have a geoserver running on http://localhost:9090/geoserver.
 *
 * @author Niels Charlier
 */
public class AppSchemaRemotePublicationTaskTest extends AbstractTaskManagerTest {

    private static final String ATT_LAYER = "layer";
    private static final String ATT_EXT_GS = "geoserver";
    private static final String ATT_FAIL = "fail";
    private static final String ATT_FILE_SERVICE = "fileService";
    private static final String ATT_FILE = "file";
    private static final String ATT_DATABASE = "database";

    @Autowired private LookupService<ExternalGS> extGeoservers;

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    @Autowired private LookupService<FileService> fileServices;

    private Configuration config;

    private Batch batch;

    @Override
    public boolean setupDataDirectory() throws Exception {
        Map<String, Serializable> params = new HashMap<>();

        FileService fileService = fileServices.get("data-directory");
        if (!fileService.checkFileExists("appschema/GeologicUnit.zip")) {
            try (InputStream in =
                    getClass().getResource("appschema/GeologicUnit.zip").openStream()) {
                fileService.create("appschema/GeologicUnit.zip", in);
            }
        }

        params.put(AppSchemaDataAccessFactory.URL.key, "file:data/MappedFeature.xml");
        params.put(AppSchemaDataAccessFactory.DBTYPE.key, AppSchemaDataAccessFactory.DBTYPE_STRING);
        DATA_DIRECTORY.addCustomType(
                new QName("urn:cgi:xmlns:CGI:GeoSciML:2.0", "MappedFeature", "gsml"), params);

        params.put(AppSchemaDataAccessFactory.URL.key, "file:data/GeologicUnit.xml");
        params.put(AppSchemaDataAccessFactory.DBTYPE.key, AppSchemaDataAccessFactory.DBTYPE_STRING);
        DATA_DIRECTORY.addCustomType(
                new QName("urn:cgi:xmlns:CGI:GeoSciML:2.0", "GeologicUnit", "gsml"), params);

        return true;
    }

    @Before
    public void setupBatch() throws Exception {
        Assume.assumeTrue(extGeoservers.get("mygs").getRESTManager().getReader().existGeoserver());

        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(AppSchemaRemotePublicationTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, AppSchemaRemotePublicationTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.setTaskParameterToAttribute(
                task1, AppSchemaRemotePublicationTaskTypeImpl.PARAM_EXT_GS, ATT_EXT_GS);
        dataUtil.setTaskParameterToAttribute(
                task1, AppSchemaRemotePublicationTaskTypeImpl.PARAM_FILE_SERVICE, ATT_FILE_SERVICE);
        dataUtil.setTaskParameterToAttribute(
                task1, AppSchemaRemotePublicationTaskTypeImpl.PARAM_FILE, ATT_FILE);
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
        if (batch != null) {
            dao.delete(batch);
        }
        if (config != null) {
            dao.delete(config);
        }
    }

    @Test
    public void testSuccessAndCleanup()
            throws SchedulerException, SQLException, MalformedURLException {
        // set some metadata
        FeatureTypeInfo fi = geoServer.getCatalog().getFeatureTypeByName("GeologicUnit");
        fi.setTitle("my title ë");
        fi.setAbstract("my abstract ë");
        geoServer.getCatalog().save(fi);

        dataUtil.setConfigurationAttribute(config, ATT_LAYER, "GeologicUnit");
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        dataUtil.setConfigurationAttribute(config, ATT_DATABASE, "myjndidb");
        dataUtil.setConfigurationAttribute(config, ATT_FILE_SERVICE, "data-directory");
        dataUtil.setConfigurationAttribute(config, ATT_FILE, "appschema/GeologicUnit.zip");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertTrue(restManager.getReader().existsDatastore("gsml", "gsml"));
        assertTrue(restManager.getReader().existsFeatureType("gsml", "gsml", "GeologicUnit"));
        assertTrue(restManager.getReader().existsLayer("gsml", "GeologicUnit", true));

        RESTLayer layer = restManager.getReader().getLayer("gsml", "GeologicUnit");
        RESTFeatureType ft = restManager.getReader().getFeatureType(layer);
        assertEquals(fi.getTitle(), ft.getTitle());
        assertEquals(fi.getAbstract(), ft.getAbstract());

        assertTrue(taskUtil.cleanup(config));

        assertFalse(restManager.getReader().existsDatastore("gsml", "gsml"));
        assertFalse(restManager.getReader().existsFeatureType("gsml", "gsml", "GeologicUnit"));
        assertFalse(restManager.getReader().existsLayer("gsml", "GeologicUnit", true));
    }

    @Test
    public void testRollback() throws SchedulerException, SQLException, MalformedURLException {
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task2, TestTaskTypeImpl.PARAM_FAIL, ATT_FAIL);
        dataUtil.addTaskToConfiguration(config, task2);

        dataUtil.setConfigurationAttribute(config, ATT_LAYER, "GeologicUnit");
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        dataUtil.setConfigurationAttribute(config, ATT_DATABASE, "myjndidb");
        dataUtil.setConfigurationAttribute(config, ATT_FILE_SERVICE, "data-directory");
        dataUtil.setConfigurationAttribute(config, ATT_FILE, "appschema/GeologicUnit.zip");
        dataUtil.setConfigurationAttribute(config, ATT_FAIL, Boolean.TRUE.toString());
        config = dao.save(config);
        task2 = config.getTasks().get("task2");
        dataUtil.addBatchElement(batch, task2);
        batch = bjService.saveAndSchedule(batch);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertFalse(restManager.getReader().existsCoveragestore("gsml", "MappedFeature"));
        assertFalse(restManager.getReader().existsCoverage("gsml", "gsml", "MappedFeature"));
        assertFalse(restManager.getReader().existsLayer("gsml", "MappedFeature", true));
    }
}
