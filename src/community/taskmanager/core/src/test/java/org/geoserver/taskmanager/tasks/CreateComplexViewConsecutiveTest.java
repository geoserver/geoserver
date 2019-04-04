/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.util.LookupService;
import org.geoserver.taskmanager.util.SqlUtil;
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

/**
 * Test if we temp values are use for consecutive views.
 *
 * @author Timothy De Bock
 */
public class CreateComplexViewConsecutiveTest extends AbstractTaskManagerTest {

    // configure these constants
    private static final String DB_NAME = "testsourcedb";
    private static final String TABLE_NAME = "gw_beleid.grondwaterlichamen_new";
    private static final String VIEW_NAME = "gw_beleid.vw_grondwaterlichamen_test";
    private static final String DEFINITION = " select * from ${table_name} where gwl like 'BL%'";

    private static final String DEFINITION_STEP2 =
            " select dataengine_id from ${table_name_step2} where gwl like 'BL%'";
    private static final String VIEW_NAME_STEP2 = "gw_beleid.vw_grondwaterlichamen_from_view";

    // attributes
    private static final String ATT_DB_NAME = "db";
    private static final String ATT_TABLE_NAME = "table_name";
    private static final String ATT_VIEW_NAME = "view_name";
    private static final String ATT_DEFINITION = "definition";

    private static final String ATT_DEFINITION_STEP2 = "definition_step2";
    private static final String ATT_TABLE_NAME_STEP2 = "table_name_step2";
    private static final String ATT_VIEW_NAME_STEP2 = "view_name_step2";

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private BatchJobService bjService;

    @Autowired private LookupService<DbSource> dbSources;

    @Autowired private Scheduler scheduler;

    private Configuration config;

    private Batch batch;

    @Before
    public void setupBatch() {
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task taskOtherStepTwo =
                generateCreateViewTask("taskOther", ATT_VIEW_NAME_STEP2, ATT_DEFINITION_STEP2);
        dataUtil.addTaskToConfiguration(config, taskOtherStepTwo);
        Task taskOtherStepOne = generateCreateViewTask("taskStep1", ATT_VIEW_NAME, ATT_DEFINITION);
        dataUtil.addTaskToConfiguration(config, taskOtherStepOne);
        config = dao.save(config);
        taskOtherStepOne = config.getTasks().get("taskStep1");
        taskOtherStepTwo = config.getTasks().get("taskOther");

        batch = fac.createBatch();
        batch.setName("my_batch_other");
        dataUtil.addBatchElement(batch, taskOtherStepOne);
        dataUtil.addBatchElement(batch, taskOtherStepTwo);

        batch = bjService.saveAndSchedule(batch);

        config = dao.init(config);
    }

    @After
    public void clearDataFromDatabase() {
        dao.delete(batch);
        dao.delete(config);
    }

    @Test
    public void testComplexViewFromOtherView() throws SchedulerException, SQLException {
        dataUtil.setConfigurationAttribute(config, ATT_DB_NAME, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_VIEW_NAME, VIEW_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_DEFINITION, DEFINITION);

        dataUtil.setConfigurationAttribute(config, ATT_DEFINITION_STEP2, DEFINITION_STEP2);
        dataUtil.setConfigurationAttribute(config, ATT_VIEW_NAME_STEP2, VIEW_NAME_STEP2);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME_STEP2, VIEW_NAME);

        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertTrue(viewExists(SqlUtil.schema(VIEW_NAME), SqlUtil.notQualified(VIEW_NAME)));

        assertFalse(viewExists(SqlUtil.schema(VIEW_NAME_STEP2), "_temp%"));
        assertTrue(
                viewExists(SqlUtil.schema(VIEW_NAME_STEP2), SqlUtil.notQualified(VIEW_NAME_STEP2)));

        assertTrue(taskUtil.cleanup(config));
        assertFalse(
                viewExists(SqlUtil.schema(VIEW_NAME_STEP2), SqlUtil.notQualified(VIEW_NAME_STEP2)));
    }

    private boolean viewExists(String schema, String pattern) throws SQLException {
        DbSource ds = dbSources.get(DB_NAME);
        try (Connection conn = ds.getDataSource().getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            if (md.storesUpperCaseIdentifiers()) {
                schema = schema.toUpperCase();
                pattern = pattern.toUpperCase();
            }
            ResultSet rs = md.getTables(null, schema, pattern, new String[] {"VIEW"});
            return (rs.next());
        }
    }

    private Task generateCreateViewTask(String taskname, String viewName, String definition) {
        Task task = fac.createTask();
        task.setName(taskname);
        task.setType(CreateComplexViewTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task, CreateComplexViewTaskTypeImpl.PARAM_DB_NAME, ATT_DB_NAME);
        dataUtil.setTaskParameterToAttribute(
                task, CreateComplexViewTaskTypeImpl.PARAM_VIEW_NAME, viewName);
        dataUtil.setTaskParameterToAttribute(
                task, CreateComplexViewTaskTypeImpl.PARAM_DEFINITION, definition);

        return task;
    }
}
