/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.ExternalGS;
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
 * Tests temp values with commit and rollback, in this case of copy table followed by create view.
 * (The create view must use the temp table from the copy table).
 *
 * @author Niels Charlier
 */
public class ConsecutiveTest extends AbstractTaskManagerTest {
    // configure these constants
    private static final String SOURCEDB_NAME = "testsourcedb";
    private static final String TARGETDB_NAME = "testtargetdb";
    private static final String TARGETDB_PUB_NAME = "mypostgresdb";
    private static final String TABLE_NAME = "gw_beleid.grondwaterlichamen_new";
    private static final String VIEW_NAME = "gw_beleid.vw_grondwaterlichamen";
    private static final String SELECT = "\"DATAENGINE_ID\"";
    private static final String WHERE = "\"GWL\" like 'BL%'";
    private static final String LAYER_NAME = "grondwaterlichamen";
    private static final int NUMBER_OF_RECORDS = 7;
    private static final int NUMBER_OF_COLUMNS = 1;

    // attributes
    private static final String ATT_SOURCE_DB = "source_db";
    private static final String ATT_TARGET_DB = "target_db";
    private static final String ATT_TABLE_NAME = "table_name";
    private static final String ATT_VIEW_NAME = "view_name";
    private static final String ATT_SELECT = "select";
    private static final String ATT_WHERE = "where";
    private static final String ATT_LAYER = "layer";
    private static final String ATT_FAIL = "fail";
    private static final String ATT_EXT_GS = "ext_gs";

    @Autowired private LookupService<ExternalGS> extGeoservers;

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private BatchJobService bjService;

    @Autowired private LookupService<DbSource> dbSources;

    @Autowired private Scheduler scheduler;

    @Autowired private Catalog catalog;

    private Configuration config;

    private Batch batch;

    @Override
    public boolean setupDataDirectory() throws Exception {
        return true;
    }

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
                task1, CopyTableTaskTypeImpl.PARAM_TARGET_TABLE_NAME, ATT_TABLE_NAME);
        dataUtil.addTaskToConfiguration(config, task1);

        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(CreateViewTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task2, CreateViewTaskTypeImpl.PARAM_DB_NAME, ATT_TARGET_DB);
        dataUtil.setTaskParameterToAttribute(
                task2, CreateViewTaskTypeImpl.PARAM_TABLE_NAME, ATT_TABLE_NAME);
        dataUtil.setTaskParameterToAttribute(
                task2, CreateViewTaskTypeImpl.PARAM_VIEW_NAME, ATT_VIEW_NAME);
        dataUtil.setTaskParameterToAttribute(
                task2, CreateViewTaskTypeImpl.PARAM_SELECT, ATT_SELECT);
        dataUtil.setTaskParameterToAttribute(task2, CreateViewTaskTypeImpl.PARAM_WHERE, ATT_WHERE);
        dataUtil.addTaskToConfiguration(config, task2);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");

        batch = fac.createBatch();

        batch.setName("my_batch");
        dataUtil.addBatchElement(batch, task1);
        dataUtil.addBatchElement(batch, task2);

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
    public void testSuccessAndCleanup() throws SchedulerException, SQLException {
        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_DB, SOURCEDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_DB, TARGETDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_VIEW_NAME, VIEW_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_SELECT, SELECT);
        dataUtil.setConfigurationAttribute(config, ATT_WHERE, WHERE);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertFalse(viewOrTableExists(SqlUtil.schema(VIEW_NAME), "_temp%"));
        assertTrue(viewOrTableExists(SqlUtil.schema(TABLE_NAME), SqlUtil.notQualified(TABLE_NAME)));
        assertTrue(viewOrTableExists(SqlUtil.schema(VIEW_NAME), SqlUtil.notQualified(VIEW_NAME)));
        assertEquals(NUMBER_OF_RECORDS, getNumberOfRecords(VIEW_NAME));
        assertEquals(NUMBER_OF_COLUMNS, getNumberOfColumns(VIEW_NAME));

        assertTrue(taskUtil.cleanup(config));
        assertFalse(
                viewOrTableExists(SqlUtil.schema(TABLE_NAME), SqlUtil.notQualified(TABLE_NAME)));
        assertFalse(viewOrTableExists(SqlUtil.schema(VIEW_NAME), SqlUtil.notQualified(VIEW_NAME)));
    }

    @Test
    public void testRollback() throws SchedulerException, SQLException {
        Task task3 = fac.createTask();
        task3.setName("task3");
        task3.setType(TestTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task3, TestTaskTypeImpl.PARAM_FAIL, ATT_FAIL);
        dataUtil.addTaskToConfiguration(config, task3);
        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_DB, SOURCEDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_DB, TARGETDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_VIEW_NAME, VIEW_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_SELECT, "*");
        dataUtil.setConfigurationAttribute(config, ATT_FAIL, Boolean.TRUE.toString());
        config = dao.save(config);
        task3 = config.getTasks().get("task3");
        dataUtil.addBatchElement(batch, task3);
        batch = bjService.saveAndSchedule(batch);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertFalse(
                viewOrTableExists(SqlUtil.schema(TABLE_NAME), SqlUtil.notQualified(TABLE_NAME)));
        assertFalse(viewOrTableExists(SqlUtil.schema(VIEW_NAME), SqlUtil.notQualified(VIEW_NAME)));
    }

    @Test
    public void testPublishSuccessAndCleanup() throws SchedulerException, SQLException {
        try (Connection conn = dbSources.get(TARGETDB_PUB_NAME).getDataSource().getConnection()) {
        } catch (SQLException e) {
            Assume.assumeTrue(false);
        }

        Task task3 = fac.createTask();
        task3.setName("task3");
        task3.setType(DbLocalPublicationTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task3, DbLocalPublicationTaskTypeImpl.PARAM_DB_NAME, ATT_TARGET_DB);
        dataUtil.setTaskParameterToAttribute(
                task3, DbLocalPublicationTaskTypeImpl.PARAM_TABLE_NAME, ATT_VIEW_NAME);
        dataUtil.setTaskParameterToAttribute(
                task3, DbLocalPublicationTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.addTaskToConfiguration(config, task3);

        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_DB, SOURCEDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_DB, TARGETDB_PUB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_VIEW_NAME, VIEW_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_SELECT, SELECT);
        dataUtil.setConfigurationAttribute(config, ATT_WHERE, WHERE);
        dataUtil.setConfigurationAttribute(config, ATT_LAYER, LAYER_NAME);
        config = dao.save(config);
        task3 = config.getTasks().get("task3");
        dataUtil.addBatchElement(batch, task3);
        batch = bjService.saveAndSchedule(batch);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertFalse(viewOrTableExists(TARGETDB_PUB_NAME, SqlUtil.schema(VIEW_NAME), "_temp%"));
        assertTrue(
                viewOrTableExists(
                        TARGETDB_PUB_NAME,
                        SqlUtil.schema(TABLE_NAME),
                        SqlUtil.notQualified(TABLE_NAME)));
        assertTrue(
                viewOrTableExists(
                        TARGETDB_PUB_NAME,
                        SqlUtil.schema(VIEW_NAME),
                        SqlUtil.notQualified(VIEW_NAME)));
        assertEquals(NUMBER_OF_RECORDS, getNumberOfRecords(TARGETDB_PUB_NAME, VIEW_NAME));
        assertEquals(NUMBER_OF_COLUMNS, getNumberOfColumns(TARGETDB_PUB_NAME, VIEW_NAME));

        assertNotNull(catalog.getLayerByName(LAYER_NAME));
        FeatureTypeInfo fti = catalog.getResourceByName(LAYER_NAME, FeatureTypeInfo.class);
        assertNotNull(fti);
        assertEquals(SqlUtil.notQualified(VIEW_NAME), fti.getNativeName());

        assertTrue(taskUtil.cleanup(config));
        assertFalse(
                viewOrTableExists(
                        TARGETDB_PUB_NAME,
                        SqlUtil.schema(TABLE_NAME),
                        SqlUtil.notQualified(TABLE_NAME)));
        assertFalse(
                viewOrTableExists(
                        TARGETDB_PUB_NAME,
                        SqlUtil.schema(VIEW_NAME),
                        SqlUtil.notQualified(VIEW_NAME)));
        assertNull(catalog.getLayerByName(LAYER_NAME));
        assertNull(catalog.getResourceByName(LAYER_NAME, FeatureTypeInfo.class));
    }

    @Test
    public void testRemotePublishSuccessAndCleanup()
            throws SchedulerException, SQLException, MalformedURLException {
        Assume.assumeTrue(extGeoservers.get("mygs").getRESTManager().getReader().existGeoserver());
        try (Connection conn = dbSources.get(TARGETDB_PUB_NAME).getDataSource().getConnection()) {
        } catch (SQLException e) {
            Assume.assumeTrue(false);
        }

        Task task3 = fac.createTask();
        task3.setName("task3");
        task3.setType(DbLocalPublicationTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task3, DbLocalPublicationTaskTypeImpl.PARAM_DB_NAME, ATT_TARGET_DB);
        dataUtil.setTaskParameterToAttribute(
                task3, DbLocalPublicationTaskTypeImpl.PARAM_TABLE_NAME, ATT_VIEW_NAME);
        dataUtil.setTaskParameterToAttribute(
                task3, DbLocalPublicationTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.addTaskToConfiguration(config, task3);

        Task task4 = fac.createTask();
        task4.setName("task4");
        task4.setType(DbRemotePublicationTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task4, DbRemotePublicationTaskTypeImpl.PARAM_EXT_GS, ATT_EXT_GS);
        dataUtil.setTaskParameterToAttribute(
                task4, DbRemotePublicationTaskTypeImpl.PARAM_DB_NAME, ATT_TARGET_DB);
        dataUtil.setTaskParameterToAttribute(
                task4, DbRemotePublicationTaskTypeImpl.PARAM_TABLE_NAME, ATT_VIEW_NAME);
        dataUtil.setTaskParameterToAttribute(
                task4, DbRemotePublicationTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.addTaskToConfiguration(config, task4);

        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_DB, SOURCEDB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_DB, TARGETDB_PUB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_TABLE_NAME, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_VIEW_NAME, VIEW_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_SELECT, SELECT);
        dataUtil.setConfigurationAttribute(config, ATT_WHERE, WHERE);
        dataUtil.setConfigurationAttribute(config, ATT_LAYER, LAYER_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        config = dao.save(config);
        task3 = config.getTasks().get("task3");
        dataUtil.addBatchElement(batch, task3);
        task4 = config.getTasks().get("task4");
        dataUtil.addBatchElement(batch, task4);
        batch = bjService.saveAndSchedule(batch);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertFalse(viewOrTableExists(TARGETDB_PUB_NAME, SqlUtil.schema(VIEW_NAME), "_temp%"));
        assertTrue(
                viewOrTableExists(
                        TARGETDB_PUB_NAME,
                        SqlUtil.schema(TABLE_NAME),
                        SqlUtil.notQualified(TABLE_NAME)));
        assertTrue(
                viewOrTableExists(
                        TARGETDB_PUB_NAME,
                        SqlUtil.schema(VIEW_NAME),
                        SqlUtil.notQualified(VIEW_NAME)));
        assertEquals(NUMBER_OF_RECORDS, getNumberOfRecords(TARGETDB_PUB_NAME, VIEW_NAME));
        assertEquals(NUMBER_OF_COLUMNS, getNumberOfColumns(TARGETDB_PUB_NAME, VIEW_NAME));

        assertNotNull(catalog.getLayerByName(LAYER_NAME));
        FeatureTypeInfo fti = catalog.getResourceByName(LAYER_NAME, FeatureTypeInfo.class);
        assertNotNull(fti);
        assertEquals(SqlUtil.notQualified(VIEW_NAME), fti.getNativeName());

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertTrue(restManager.getReader().existsLayer("gs", LAYER_NAME, true));
        assertEquals(
                SqlUtil.notQualified(VIEW_NAME),
                restManager
                        .getReader()
                        .getFeatureType(restManager.getReader().getLayer("gs", LAYER_NAME))
                        .getNativeName());

        // cleanup
        assertTrue(taskUtil.cleanup(config));

        assertFalse(
                viewOrTableExists(
                        TARGETDB_PUB_NAME,
                        SqlUtil.schema(TABLE_NAME),
                        SqlUtil.notQualified(TABLE_NAME)));
        assertFalse(
                viewOrTableExists(
                        TARGETDB_PUB_NAME,
                        SqlUtil.schema(VIEW_NAME),
                        SqlUtil.notQualified(VIEW_NAME)));
        assertNull(catalog.getLayerByName(LAYER_NAME));
        assertNull(catalog.getResourceByName(LAYER_NAME, FeatureTypeInfo.class));
        assertFalse(restManager.getReader().existsLayer("gs", LAYER_NAME, true));
    }

    private int getNumberOfRecords(String tableName) throws SQLException {
        return getNumberOfRecords(TARGETDB_NAME, tableName);
    }

    private int getNumberOfRecords(String dbName, String tableName) throws SQLException {
        DbSource ds = dbSources.get(dbName);
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
        return getNumberOfColumns(TARGETDB_NAME, tableName);
    }

    private int getNumberOfColumns(String dbName, String tableName) throws SQLException {
        DbSource ds = dbSources.get(dbName);
        try (Connection conn = ds.getDataSource().getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
                    return rs.getMetaData().getColumnCount();
                }
            }
        }
    }

    private boolean viewOrTableExists(String schema, String pattern) throws SQLException {
        return viewOrTableExists(TARGETDB_NAME, schema, pattern);
    }

    private boolean viewOrTableExists(String dbName, String schema, String pattern)
            throws SQLException {
        DbSource ds = dbSources.get(dbName);
        try (Connection conn = ds.getDataSource().getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            if (md.storesUpperCaseIdentifiers()) {
                schema = schema.toUpperCase();
                pattern = pattern.toUpperCase();
            }
            ResultSet rs = md.getTables(null, schema, pattern, new String[] {"VIEW", "TABLE"});
            return (rs.next());
        }
    }
}
