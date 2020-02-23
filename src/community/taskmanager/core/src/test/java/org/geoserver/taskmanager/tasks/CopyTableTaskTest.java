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
 * @author Niels Charlier
 * @author Timothy De Bock
 */
public class CopyTableTaskTest extends AbstractTaskManagerTest {

    // configure these constants
    private static final String SOURCEDB_NAME = "testsourcedb";
    private static final String TARGETDB_NAME = "testtargetdb";
    private static final String SOURCEDB_PG_NAME = "myjndidb";
    private static final String TARGETDB_PG_NAME = "mypostgresdb";
    private static final String TABLE_NAME = "gw_beleid.grondwaterlichamen_new";
    private static final String TARGET_TABLE_NAME = "temp.grondwaterlichamen_copy";

    private static final String VIEW_NAME = "gw_beleid.vw_grondwaterlichamen";

    private static final String VIEW_W_GENERATED_ID =
            "gw_beleid.vw_grondwaterlichamen_generated_id";

    private static final String VIEW_CAMEL_CASE = "gw_beleid.vw_GrondwaterlichamenCamelCase";
    private static final String TARGET_TABLE_CAMELCASE_NAME = "temp.Grondwaterlichamen_Copy";

    private static final String TARGET_TABLE_FROM_VIEW_NAME = "temp.grondwaterlichamen_vw_copy";

    private static final String TARGET_TABLE_NAME_NEW_SCHEMA = "foobar.grondwaterlichamen_copy";

    // attributes
    private static final String ATT_TABLE_NAME = "table_name";
    private static final String ATT_TARGET_DB = "target_db";
    private static final String ATT_SOURCE_DB = "source_db";
    private static final String ATT_TARGET_TABLE_NAME = "target_table_name";

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
        task1.setType(CopyTableTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, CopyTableTaskTypeImpl.PARAM_SOURCE_DB_NAME, ATT_SOURCE_DB);
        dataUtil.setTaskParameterToAttribute(
                task1, CopyTableTaskTypeImpl.PARAM_TARGET_DB_NAME, ATT_TARGET_DB);
        dataUtil.setTaskParameterToAttribute(
                task1, CopyTableTaskTypeImpl.PARAM_TABLE_NAME, ATT_TABLE_NAME);

        dataUtil.setTaskParameterToAttribute(
                task1, CopyTableTaskTypeImpl.PARAM_TARGET_TABLE_NAME, ATT_TARGET_TABLE_NAME);
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
    public void testTableSuccess() throws SchedulerException, SQLException {
        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_DB, SOURCEDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_DB, TARGETDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_TABLE_NAME, TARGET_TABLE_NAME);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        String[] splitTargetTableName = TARGET_TABLE_NAME.split("\\.", 2);
        if (splitTargetTableName.length == 2) {
            assertFalse(tableExists(TARGETDB_NAME, splitTargetTableName[0], "_temp%"));
            assertTrue(
                    tableExists(TARGETDB_NAME, splitTargetTableName[0], splitTargetTableName[1]));
        } else {
            assertFalse(tableExists(TARGETDB_NAME, null, "_temp%"));
            assertTrue(tableExists(TARGETDB_NAME, null, TARGET_TABLE_NAME));
        }
        assertEquals(
                getNumberOfRecords(SOURCEDB_NAME, TABLE_NAME),
                getNumberOfRecords(TARGETDB_NAME, TARGET_TABLE_NAME));
        assertEquals(
                getNumberOfColumns(SOURCEDB_NAME, TABLE_NAME),
                getNumberOfColumns(TARGETDB_NAME, TARGET_TABLE_NAME));

        String[] splitTableName = TABLE_NAME.split("\\.", 2);

        int numberOfindexesSource = getNumberOfIndexes(SOURCEDB_NAME, splitTableName[1]);
        int numberOfindexesTarget = getNumberOfIndexes(TARGETDB_NAME, splitTargetTableName[1]);
        assertEquals(numberOfindexesSource, numberOfindexesTarget);

        assertTrue(taskUtil.cleanup(config));

        if (splitTargetTableName.length == 2) {
            assertFalse(
                    tableExists(TARGETDB_NAME, splitTargetTableName[0], splitTargetTableName[1]));
        } else {
            assertFalse(tableExists(TARGETDB_NAME, null, TARGET_TABLE_NAME));
        }
    }

    @Test
    public void testCopyFromViewSuccess() throws SchedulerException, SQLException {
        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_DB, SOURCEDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_DB, TARGETDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, VIEW_NAME);
        dataUtil.setConfigurationAttribute(
                config, ATT_TARGET_TABLE_NAME, TARGET_TABLE_FROM_VIEW_NAME);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {
            // waiting to be done.
        }

        String[] split = TARGET_TABLE_FROM_VIEW_NAME.split("\\.", 2);
        if (split.length == 2) {
            assertFalse(tableExists(TARGETDB_NAME, split[0], "_temp%"));
            assertTrue(tableExists(TARGETDB_NAME, split[0], split[1]));
        } else {
            assertFalse(tableExists(TARGETDB_NAME, null, "_temp%"));
            assertTrue(tableExists(TARGETDB_NAME, null, TARGET_TABLE_FROM_VIEW_NAME));
        }
        int numberOfRecordsSource = getNumberOfRecords(SOURCEDB_NAME, VIEW_NAME);
        int numberOfRecordsTarget = getNumberOfRecords(TARGETDB_NAME, TARGET_TABLE_FROM_VIEW_NAME);
        assertEquals(numberOfRecordsSource, numberOfRecordsTarget);
        // a primary key column was added
        assertEquals(
                getNumberOfColumns(SOURCEDB_NAME, VIEW_NAME) + 1,
                getNumberOfColumns(TARGETDB_NAME, TARGET_TABLE_FROM_VIEW_NAME));

        int numberOfindexesTarget = getNumberOfIndexes(TARGETDB_NAME, split[1]);

        int numberOfColumnsTarget = getNumberOfColumns(TARGETDB_NAME, TARGET_TABLE_FROM_VIEW_NAME);
        // We did not add Geometry support on the H2 DB. So only check the index on the shape
        if (numberOfColumnsTarget == 5) {
            assertEquals(2, numberOfindexesTarget);
            assertEquals("shape", getColumnName(TARGETDB_NAME, TARGET_TABLE_FROM_VIEW_NAME, 3));
        } else {
            assertEquals(1, numberOfindexesTarget);
        }

        assertTrue(taskUtil.cleanup(config));

        if (split.length == 2) {
            assertFalse(tableExists(TARGETDB_NAME, split[0], split[1]));
        } else {
            assertFalse(tableExists(TARGETDB_NAME, null, TARGET_TABLE_NAME));
        }
    }

    /** Use the existing generated_id column if it exists. */
    @Test
    public void testCopyViewWithGeneratedIdColumn() throws SchedulerException, SQLException {
        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_DB, SOURCEDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_DB, TARGETDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, VIEW_W_GENERATED_ID);
        dataUtil.setConfigurationAttribute(
                config, ATT_TARGET_TABLE_NAME, TARGET_TABLE_FROM_VIEW_NAME);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {
            // waiting to be done.
        }

        String[] split = TARGET_TABLE_FROM_VIEW_NAME.split("\\.", 2);
        if (split.length == 2) {
            assertFalse(tableExists(TARGETDB_NAME, split[0], "_temp%"));
            assertTrue(tableExists(TARGETDB_NAME, split[0], split[1]));
        } else {
            assertFalse(tableExists(TARGETDB_NAME, null, "_temp%"));
            assertTrue(tableExists(TARGETDB_NAME, null, TARGET_TABLE_FROM_VIEW_NAME));
        }
        assertEquals(
                getNumberOfRecords(SOURCEDB_NAME, VIEW_NAME),
                getNumberOfRecords(TARGETDB_NAME, TARGET_TABLE_FROM_VIEW_NAME));
        // a primary key column was not added
        assertEquals(
                getNumberOfColumns(SOURCEDB_NAME, VIEW_NAME),
                getNumberOfColumns(TARGETDB_NAME, TARGET_TABLE_FROM_VIEW_NAME));

        assertTrue(taskUtil.cleanup(config));

        if (split.length == 2) {
            assertFalse(tableExists(TARGETDB_NAME, split[0], split[1]));
        } else {
            assertFalse(tableExists(TARGETDB_NAME, null, TARGET_TABLE_NAME));
        }
    }

    /** keep the case of columns. */
    @Test
    public void testCopyKeepCase() throws SchedulerException, SQLException {
        DbSource source = dbSources.get(SOURCEDB_PG_NAME);
        try (Connection conn = source.getDataSource().getConnection()) {
            try (ResultSet res =
                    conn.getMetaData()
                            .getTables(
                                    null,
                                    SqlUtil.schema(VIEW_CAMEL_CASE),
                                    SqlUtil.notQualified(VIEW_CAMEL_CASE),
                                    null)) {
                Assume.assumeTrue(res.next());
            }
        } catch (SQLException e) {
            Assume.assumeTrue(false);
        }
        DbSource target = dbSources.get(TARGETDB_PG_NAME);
        try (Connection conn = target.getDataSource().getConnection()) {
        } catch (SQLException e) {
            Assume.assumeTrue(false);
        }

        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_DB, SOURCEDB_PG_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_DB, TARGETDB_PG_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, VIEW_CAMEL_CASE);
        dataUtil.setConfigurationAttribute(
                config, ATT_TARGET_TABLE_NAME, TARGET_TABLE_CAMELCASE_NAME);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {
            // waiting to be done.
        }

        String[] split = TARGET_TABLE_CAMELCASE_NAME.split("\\.", 2);
        if (split.length == 2) {
            assertFalse(tableExists(TARGETDB_PG_NAME, split[0], "_temp%"));
            assertTrue(tableExists(TARGETDB_PG_NAME, split[0], split[1]));
        } else {
            assertFalse(tableExists(TARGETDB_PG_NAME, null, "_temp%"));
            assertTrue(tableExists(TARGETDB_PG_NAME, null, TARGET_TABLE_CAMELCASE_NAME));
        }
        // column names are the same
        assertEquals(
                getColumnName(SOURCEDB_PG_NAME, VIEW_CAMEL_CASE, 1),
                getColumnName(TARGETDB_PG_NAME, TARGET_TABLE_CAMELCASE_NAME, 1));
        assertEquals(
                getColumnName(SOURCEDB_PG_NAME, VIEW_CAMEL_CASE, 2),
                getColumnName(TARGETDB_PG_NAME, TARGET_TABLE_CAMELCASE_NAME, 2));

        assertTrue(taskUtil.cleanup(config));

        if (split.length == 2) {
            assertFalse(tableExists(TARGETDB_PG_NAME, split[0], split[1]));
        } else {
            assertFalse(tableExists(TARGETDB_PG_NAME, null, TARGET_TABLE_NAME));
        }
    }

    @Test
    public void testRollback() throws SchedulerException, SQLException {
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task2, TestTaskTypeImpl.PARAM_FAIL, "fail");
        dataUtil.addTaskToConfiguration(config, task2);
        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_DB, SOURCEDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_DB, TARGETDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_TABLE_NAME, TARGET_TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, "fail", "true");
        config = dao.save(config);
        task2 = config.getTasks().get("task2");
        dataUtil.addBatchElement(batch, task2);
        batch = bjService.saveAndSchedule(batch);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertFalse(tableExists(TARGETDB_NAME, SqlUtil.schema(TARGET_TABLE_NAME), "_temp%"));
        assertFalse(
                tableExists(
                        TARGETDB_NAME,
                        SqlUtil.schema(TARGET_TABLE_NAME),
                        SqlUtil.notQualified(TARGET_TABLE_NAME)));
    }

    /** the copy task should create the schema if it doesn't exist. */
    @Test
    public void testTableInNewSchema() throws SchedulerException, SQLException {
        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_DB, SOURCEDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_DB, TARGETDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(
                config, ATT_TARGET_TABLE_NAME, TARGET_TABLE_NAME_NEW_SCHEMA);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        String[] splitTargetTableName = TARGET_TABLE_NAME_NEW_SCHEMA.split("\\.", 2);
        if (splitTargetTableName.length == 2) {
            assertFalse(tableExists(TARGETDB_NAME, splitTargetTableName[0], "_temp%"));
            assertTrue(
                    tableExists(TARGETDB_NAME, splitTargetTableName[0], splitTargetTableName[1]));
        } else {
            assertFalse(tableExists(TARGETDB_NAME, null, "_temp%"));
            assertTrue(tableExists(TARGETDB_NAME, null, TARGET_TABLE_NAME_NEW_SCHEMA));
        }

        assertTrue(taskUtil.cleanup(config));

        if (splitTargetTableName.length == 2) {
            assertFalse(
                    tableExists(TARGETDB_NAME, splitTargetTableName[0], splitTargetTableName[1]));
        } else {
            assertFalse(tableExists(TARGETDB_NAME, null, TARGET_TABLE_NAME_NEW_SCHEMA));
        }
    }

    private int getNumberOfRecords(String db, String tableName) throws SQLException {
        DbSource ds = dbSources.get(db);
        try (Connection conn = ds.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }

    private int getNumberOfColumns(String db, String tableName) throws SQLException {
        DbSource ds = dbSources.get(db);
        try (Connection conn = ds.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
                    return rs.getMetaData().getColumnCount();
                }
            }
        }
    }

    private String getColumnName(String db, String tableName, int columnIndex) throws SQLException {
        DbSource ds = dbSources.get(db);
        try (Connection conn = ds.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs =
                        stmt.executeQuery("SELECT * FROM " + ds.getDialect().quote(tableName))) {
                    return rs.getMetaData().getColumnName(columnIndex);
                }
            }
        }
    }

    private int getNumberOfIndexes(String db, String tableName) throws SQLException {
        int indexCount = 0;
        DbSource ds = dbSources.get(db);
        try (Connection conn = ds.getDataSource().getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs;
            if (metaData.storesUpperCaseIdentifiers()) {
                rs = metaData.getIndexInfo(null, null, tableName.toUpperCase(), false, false);
            } else {
                rs = metaData.getIndexInfo(null, null, tableName, false, false);
            }
            while (rs.next()) {
                indexCount++;
            }
        }
        return indexCount;
    }

    private boolean tableExists(String db, String schema, String pattern) throws SQLException {
        DbSource ds = dbSources.get(db);
        try (Connection conn = ds.getDataSource().getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs;
            if (md.storesUpperCaseIdentifiers()) {
                rs =
                        md.getTables(
                                null,
                                schema.toUpperCase(),
                                pattern.toUpperCase(),
                                new String[] {"TABLE"});
            } else {
                rs = md.getTables(null, schema, pattern, new String[] {"TABLE"});
            }
            return (rs.next());
        }
    }
}
