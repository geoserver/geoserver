/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.test.TestData;
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

public class FileLocalPublicationTaskTest extends AbstractTaskManagerTest {

    // configure these constants
    private static final String FILE_LOCATION = "test/world.tiff";
    private static final String FILE_SERVICE = "data-directory";
    private static final String WORKSPACE = "gs";
    private static final String COVERAGE_NAME = "world";
    private static final String LAYER_NAME = WORKSPACE + ":" + COVERAGE_NAME;

    // attributes
    private static final String ATT_FILE_SERVICE = "fileService";
    private static final String ATT_FILE = "file";
    private static final String ATT_LAYER = "layer";
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
        if (!fileService.checkFileExists(FILE_LOCATION)) {
            fileService.create(
                    FILE_LOCATION, TestData.class.getResource("world.tiff").openStream());
        }

        // create configuration
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(FileLocalPublicationTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, FileLocalPublicationTaskTypeImpl.PARAM_FILE_SERVICE, ATT_FILE_SERVICE);
        dataUtil.setTaskParameterToAttribute(
                task1, FileLocalPublicationTaskTypeImpl.PARAM_FILE, ATT_FILE);
        dataUtil.setTaskParameterToAttribute(
                task1, FileLocalPublicationTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
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
    public void testSuccessAndCleanup() throws SchedulerException {
        dataUtil.setConfigurationAttribute(config, ATT_FILE_SERVICE, FILE_SERVICE);
        dataUtil.setConfigurationAttribute(config, ATT_FILE, FILE_LOCATION);
        dataUtil.setConfigurationAttribute(config, ATT_LAYER, LAYER_NAME);
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertNotNull(catalog.getLayerByName(LAYER_NAME));
        CoverageStoreInfo csi =
                catalog.getStoreByName(WORKSPACE, COVERAGE_NAME, CoverageStoreInfo.class);
        assertNotNull(csi);
        assertEquals(fileServices.get(FILE_SERVICE).getURI(FILE_LOCATION).toString(), csi.getURL());
        assertNotNull(catalog.getResourceByName(LAYER_NAME, CoverageInfo.class));

        taskUtil.cleanup(config);

        assertNull(catalog.getLayerByName(LAYER_NAME));
        assertNull(catalog.getStoreByName(WORKSPACE, COVERAGE_NAME, CoverageStoreInfo.class));
        assertNull(catalog.getResourceByName(LAYER_NAME, CoverageInfo.class));
    }

    @Test
    public void testRollback() throws SchedulerException {
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task2, TestTaskTypeImpl.PARAM_FAIL, ATT_FAIL);
        dataUtil.addTaskToConfiguration(config, task2);

        dataUtil.setConfigurationAttribute(config, ATT_FILE_SERVICE, FILE_SERVICE);
        dataUtil.setConfigurationAttribute(config, ATT_FILE, FILE_LOCATION);
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
        assertNull(catalog.getStoreByName(WORKSPACE, COVERAGE_NAME, CoverageStoreInfo.class));
        assertNull(catalog.getResourceByName(LAYER_NAME, CoverageInfo.class));
    }
}
