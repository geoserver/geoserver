/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.util.LookupService;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
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
 * To run this test you should have a postgres running on localhost with database 'mydb' (or
 * configure in application context), initiated with create-source.sql.
 *
 * @author Niels Charlier
 */
public class DbLocalPublicationTaskTest extends AbstractTaskManagerTest {

    // configure these constants
    // private static final String DB_NAME = "mypostgresdb";
    private static final String DB_NAME = "myjndidb";
    private static final String WORKSPACE = "gs";
    private static final String TABLE_NAME = "grondwaterlichamen_new";
    private static final String LAYER_NAME = WORKSPACE + ":grondwaterlichamen";

    // attributes
    private static final String ATT_DB_NAME = "db";
    private static final String ATT_TABLE_NAME = "table_name";
    private static final String ATT_LAYER = "layer";
    private static final String ATT_FAIL = "fail";

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    @Autowired private Catalog catalog;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private LookupService<DbSource> dbSources;

    private Configuration config;

    private Batch batch;

    @Override
    public boolean setupDataDirectory() throws Exception {
        return true;
    }

    @Before
    public void setupBatch() {
        DbSource source = dbSources.get(DB_NAME);
        try (Connection conn = source.getDataSource().getConnection()) {
            try (ResultSet res = conn.getMetaData().getTables(null, null, TABLE_NAME, null)) {
                Assume.assumeTrue(res.next());
            }
        } catch (SQLException e) {
            Assume.assumeTrue(false);
        }

        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(DbLocalPublicationTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, DbLocalPublicationTaskTypeImpl.PARAM_DB_NAME, ATT_DB_NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, DbLocalPublicationTaskTypeImpl.PARAM_TABLE_NAME, ATT_TABLE_NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, DbLocalPublicationTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.addTaskToConfiguration(config, task1);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");

        batch = fac.createBatch();

        batch.setName("my_batch");
        dataUtil.addBatchElement(batch, task1);

        batch = bjService.saveAndSchedule(batch);
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
    public void testSuccessAndCleanup() throws SchedulerException {
        dataUtil.setConfigurationAttribute(config, ATT_DB_NAME, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_LAYER, LAYER_NAME);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertNotNull(catalog.getLayerByName(LAYER_NAME));
        assertNotNull(catalog.getStoreByName(WORKSPACE, DB_NAME, DataStoreInfo.class));
        FeatureTypeInfo fti = catalog.getResourceByName(LAYER_NAME, FeatureTypeInfo.class);
        assertNotNull(fti);
        assertEquals(TABLE_NAME, fti.getNativeName());

        taskUtil.cleanup(config);

        assertNull(catalog.getLayerByName(LAYER_NAME));
        assertNull(catalog.getStoreByName(WORKSPACE, ATT_DB_NAME, DataStoreInfo.class));
        assertNull(catalog.getResourceByName(LAYER_NAME, FeatureTypeInfo.class));
    }

    @Test
    public void testRollback() throws SchedulerException {
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task2, TestTaskTypeImpl.PARAM_FAIL, ATT_FAIL);
        dataUtil.addTaskToConfiguration(config, task2);

        dataUtil.setConfigurationAttribute(config, ATT_DB_NAME, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_LAYER, LAYER_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_FAIL, Boolean.TRUE.toString());
        config = dao.save(config);
        task2 = config.getTasks().get("task2");
        dataUtil.addBatchElement(batch, task2);
        batch = bjService.saveAndSchedule(batch);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertNull(catalog.getLayerByName(LAYER_NAME));
        assertNull(catalog.getStoreByName(WORKSPACE, ATT_DB_NAME, DataStoreInfo.class));
        assertNull(catalog.getResourceByName(LAYER_NAME, FeatureTypeInfo.class));
    }
}
