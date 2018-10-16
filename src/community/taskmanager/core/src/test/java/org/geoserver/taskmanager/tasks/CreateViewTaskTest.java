/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
 * @author Niels Charlier
 * @author Timothy De Bock
 */
public class CreateViewTaskTest extends AbstractTaskManagerTest {

    // configure these constants
    private static final String DB_NAME = "testsourcedb";
    private static final String TABLE_NAME = "gw_beleid.grondwaterlichamen_new";
    private static final String VIEW_NAME = "gw_beleid.vw_grondwaterlichamen_new";
    private static final String SELECT = "dataengine_id";
    private static final String WHERE = "gwl like 'BL%'";
    private static final int NUMBER_OF_RECORDS = 7;
    private static final int NUMBER_OF_COLUMNS = 1;
    private static final String VIEW_NAME_NEW_SCHEMA = "newschema.vw_grondwaterlichamen";

    // attributes
    private static final String ATT_DB_NAME = "db";
    private static final String ATT_TABLE_NAME = "table_name";
    private static final String ATT_VIEW_NAME = "view_name";
    private static final String ATT_SELECT = "select";
    private static final String ATT_WHERE = "where";
    private static final String ATT_FAIL = "fail";

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

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(CreateViewTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, CreateViewTaskTypeImpl.PARAM_DB_NAME, ATT_DB_NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, CreateViewTaskTypeImpl.PARAM_TABLE_NAME, ATT_TABLE_NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, CreateViewTaskTypeImpl.PARAM_VIEW_NAME, ATT_VIEW_NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, CreateViewTaskTypeImpl.PARAM_SELECT, ATT_SELECT);
        dataUtil.setTaskParameterToAttribute(task1, CreateViewTaskTypeImpl.PARAM_WHERE, ATT_WHERE);
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
    public void testSimpleView() throws SchedulerException, SQLException {
        dataUtil.setConfigurationAttribute(config, ATT_DB_NAME, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_VIEW_NAME, VIEW_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_SELECT, "*");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertFalse(viewExists(SqlUtil.schema(VIEW_NAME), "_temp%"));
        assertTrue(viewExists(SqlUtil.schema(VIEW_NAME), SqlUtil.notQualified(VIEW_NAME)));
        assertEquals(getNumberOfRecords(TABLE_NAME), getNumberOfRecords(VIEW_NAME));
        assertEquals(getNumberOfColumns(TABLE_NAME), getNumberOfColumns(VIEW_NAME));

        assertTrue(taskUtil.cleanup(config));
        assertFalse(viewExists(SqlUtil.schema(VIEW_NAME), SqlUtil.notQualified(VIEW_NAME)));
    }

    @Test
    public void testComplexView() throws SchedulerException, SQLException {
        dataUtil.setConfigurationAttribute(config, ATT_DB_NAME, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_VIEW_NAME, VIEW_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_SELECT, SELECT);
        dataUtil.setConfigurationAttribute(config, ATT_WHERE, WHERE);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertFalse(viewExists(SqlUtil.schema(VIEW_NAME), "_temp%"));
        assertTrue(viewExists(SqlUtil.schema(VIEW_NAME), SqlUtil.notQualified(VIEW_NAME)));
        assertEquals(NUMBER_OF_RECORDS, getNumberOfRecords(VIEW_NAME));
        assertEquals(NUMBER_OF_COLUMNS, getNumberOfColumns(VIEW_NAME));

        assertTrue(taskUtil.cleanup(config));
        assertFalse(viewExists(SqlUtil.schema(VIEW_NAME), SqlUtil.notQualified(VIEW_NAME)));
    }

    @Test
    public void testSimpleViewInNewSchema() throws SchedulerException, SQLException {
        dataUtil.setConfigurationAttribute(config, ATT_DB_NAME, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_VIEW_NAME, VIEW_NAME_NEW_SCHEMA);
        dataUtil.setConfigurationAttribute(config, ATT_SELECT, "*");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertFalse(viewExists(SqlUtil.schema(VIEW_NAME_NEW_SCHEMA), "_temp%"));
        assertTrue(
                viewExists(
                        SqlUtil.schema(VIEW_NAME_NEW_SCHEMA),
                        SqlUtil.notQualified(VIEW_NAME_NEW_SCHEMA)));

        assertTrue(taskUtil.cleanup(config));
        assertFalse(
                viewExists(
                        SqlUtil.schema(VIEW_NAME_NEW_SCHEMA),
                        SqlUtil.notQualified(VIEW_NAME_NEW_SCHEMA)));
    }

    @Test
    public void testComplexViewInNewSchema() throws SchedulerException, SQLException {
        dataUtil.setConfigurationAttribute(config, ATT_DB_NAME, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_VIEW_NAME, VIEW_NAME_NEW_SCHEMA);
        dataUtil.setConfigurationAttribute(config, ATT_SELECT, SELECT);
        dataUtil.setConfigurationAttribute(config, ATT_WHERE, WHERE);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertFalse(viewExists(SqlUtil.schema(VIEW_NAME_NEW_SCHEMA), "_temp%"));
        assertTrue(
                viewExists(
                        SqlUtil.schema(VIEW_NAME_NEW_SCHEMA),
                        SqlUtil.notQualified(VIEW_NAME_NEW_SCHEMA)));
        assertEquals(NUMBER_OF_RECORDS, getNumberOfRecords(VIEW_NAME_NEW_SCHEMA));
        assertEquals(NUMBER_OF_COLUMNS, getNumberOfColumns(VIEW_NAME_NEW_SCHEMA));

        assertTrue(taskUtil.cleanup(config));
        assertFalse(
                viewExists(
                        SqlUtil.schema(VIEW_NAME_NEW_SCHEMA),
                        SqlUtil.notQualified(VIEW_NAME_NEW_SCHEMA)));
    }

    @Test
    public void testRollback() throws SchedulerException, SQLException {
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task2, TestTaskTypeImpl.PARAM_FAIL, ATT_FAIL);
        dataUtil.addTaskToConfiguration(config, task2);
        dataUtil.setConfigurationAttribute(config, ATT_DB_NAME, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_VIEW_NAME, VIEW_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_SELECT, "*");
        dataUtil.setConfigurationAttribute(config, ATT_FAIL, Boolean.TRUE.toString());
        config = dao.save(config);
        task2 = config.getTasks().get("task2");
        dataUtil.addBatchElement(batch, task2);
        batch = bjService.saveAndSchedule(batch);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertFalse(viewExists(SqlUtil.schema(VIEW_NAME), SqlUtil.notQualified(VIEW_NAME)));
        assertFalse(viewExists(SqlUtil.schema(VIEW_NAME), "_temp%"));
    }

    private int getNumberOfRecords(String tableName) throws SQLException {
        DbSource ds = dbSources.get(DB_NAME);
        try (Connection conn = ds.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }

    private int getNumberOfColumns(String tableName) throws SQLException {
        DbSource ds = dbSources.get(DB_NAME);
        try (Connection conn = ds.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
                    return rs.getMetaData().getColumnCount();
                }
            }
        }
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
}
