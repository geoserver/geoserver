/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.namespace.QName;
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
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.Trigger.TriggerState;
import org.springframework.beans.factory.annotation.Autowired;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;

/**
 * To run this test you should have a geoserver running on http://localhost:9090/geoserver
 * + postgres running on localhost with database 'mydb' (or configure in application context), 
 * initiated with create-source.sql.
 * 
 * @author Niels Charlier
 */
public class DbRemotePublicationTaskTest extends AbstractTaskManagerTest {
    
    //configure these constants
    private static final String DB_NAME = "mypostgresdb";
    private static final String TABLE_NAME = "vw_grondwaterlichamen";
    
    private static QName MY_TYPE = new QName(DB_NAME, TABLE_NAME, DB_NAME);
    
    private static final String ATT_LAYER = "layer";
    private static final String ATT_EXT_GS = "geoserver";
    private static final String ATT_FAIL = "fail";
    private static final String ATT_DB_NAME = "dbName";
    
    @Autowired
    private LookupService<ExternalGS> extGeoservers;
        
    @Autowired
    private TaskManagerDao dao;
    
    @Autowired
    private TaskManagerFactory fac;
    
    @Autowired
    private TaskManagerDataUtil dataUtil;

    @Autowired
    private TaskManagerTaskUtil taskUtil;
    
    @Autowired
    private BatchJobService bjService;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private LookupService<DbSource> dbSources;

    private Configuration config;
    
    private Batch batch;
    
    @Override
    public boolean setupDataDirectory() throws Exception {
        DATA_DIRECTORY.addCustomType(MY_TYPE, dbSources.get(DB_NAME).getParameters());
        return true;
    }
    
    @Before
    public void setupBatch() throws MalformedURLException {
        Assume.assumeTrue(extGeoservers.get("mygs").getRESTManager().getReader().existGeoserver());
        try (Connection conn = dbSources.get(DB_NAME).getDataSource().getConnection()) {
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
        task1.setType(DbRemotePublicationTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task1, DbRemotePublicationTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.setTaskParameterToAttribute(task1, DbRemotePublicationTaskTypeImpl.PARAM_EXT_GS, ATT_EXT_GS);
        dataUtil.setTaskParameterToAttribute(task1, DbRemotePublicationTaskTypeImpl.PARAM_DB_NAME, ATT_DB_NAME);
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
    public void testSuccessAndCleanup() throws SchedulerException, SQLException, MalformedURLException {
        dataUtil.setConfigurationAttribute(config, ATT_DB_NAME, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_LAYER, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        config = dao.save(config);
        
        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(batch.getFullName())
                .startNow()        
                .build();
        scheduler.scheduleJob(trigger);
        
        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.COMPLETE
                && scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}
        
        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();
        
        assertTrue(restManager.getReader().existsDatastore(DB_NAME, DB_NAME));
        assertTrue(restManager.getReader().existsFeatureType(DB_NAME, DB_NAME, TABLE_NAME));
        assertTrue(restManager.getReader().existsLayer(DB_NAME, TABLE_NAME, true));
        
        assertTrue(taskUtil.cleanup(config));      
        
        assertFalse(restManager.getReader().existsDatastore(DB_NAME, DB_NAME));
        assertFalse(restManager.getReader().existsFeatureType(DB_NAME, DB_NAME, TABLE_NAME));
        assertFalse(restManager.getReader().existsLayer(DB_NAME, TABLE_NAME, true));
    }
    
    @Test
    public void testRollback() throws SchedulerException, SQLException, MalformedURLException {
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task2, TestTaskTypeImpl.PARAM_FAIL, ATT_FAIL);
        dataUtil.addTaskToConfiguration(config, task2);  
        
        dataUtil.setConfigurationAttribute(config, ATT_DB_NAME, DB_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_LAYER, TABLE_NAME);
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        dataUtil.setConfigurationAttribute(config, ATT_FAIL, Boolean.TRUE.toString());
        config = dao.save(config);
        task2 = config.getTasks().get("task2");
        dataUtil.addBatchElement(batch, task2);
        batch = bjService.saveAndSchedule(batch);
        
        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(batch.getFullName())
                .startNow()        
                .build();
        scheduler.scheduleJob(trigger);
        
        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.COMPLETE
                && scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}
        
        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertFalse(restManager.getReader().existsDatastore(DB_NAME, DB_NAME));
        assertFalse(restManager.getReader().existsFeatureType(DB_NAME, DB_NAME, TABLE_NAME));
        assertFalse(restManager.getReader().existsLayer(DB_NAME, TABLE_NAME, true));
    }

}
