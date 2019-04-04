/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.external.impl.S3FileServiceImpl;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.util.LookupService;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.geotools.util.logging.Logging;
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
public class CopyS3FileTaskTest extends AbstractTaskManagerTest {

    private static final Logger LOGGER = Logging.getLogger(CopyS3FileTaskTest.class);

    // configure these constants
    private static String SOURCE_ALIAS = "test";
    private static String TARGET_ALIAS = "test";
    private static String SOURCE_BUCKET = "source";
    private static String TARGET_BUCKET = "target";
    private static String SOURCE_SERVICE = S3FileServiceImpl.name(SOURCE_ALIAS, SOURCE_BUCKET);
    private static String TARGET_SERVICE = S3FileServiceImpl.name(TARGET_ALIAS, TARGET_BUCKET);
    private static String SOURCE_FILE = "test/salinity.tif";
    private static String TARGET_FILE_PATTERN = "new/salinity.###.tif";
    private static String TARGET_FILE_OLD = "new/salinity.42.tif";
    private static String TARGET_FILE_NEW = "new/salinity.43.tif";

    private static final String ATT_SOURCE_SERVICE = "source-service";
    private static final String ATT_TARGET_SERVICE = "target-service";
    private static final String ATT_SOURCE_PATH = "source-target";
    private static final String ATT_TARGET_PATH = "target-taret";

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    @Autowired private LookupService<FileService> fileServices;

    private Configuration config;

    private Batch batch;

    @Before
    public void setupBatch() throws Exception {
        try {
            FileService sourceFileService =
                    fileServices.get(S3FileServiceImpl.name(SOURCE_ALIAS, SOURCE_BUCKET));
            Assume.assumeNotNull(sourceFileService);
            Assume.assumeTrue(
                    "File exists on s3 service", sourceFileService.checkFileExists(SOURCE_FILE));

            FileService targetFileService =
                    fileServices.get(S3FileServiceImpl.name(TARGET_ALIAS, TARGET_BUCKET));
            Assume.assumeNotNull(targetFileService);

            // copy old version
            if (!targetFileService.checkFileExists(TARGET_FILE_OLD)) {
                try (InputStream is = sourceFileService.read(SOURCE_FILE)) {
                    targetFileService.create(TARGET_FILE_OLD, is);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            Assume.assumeTrue("S3 services are configured and available", false);
        }

        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(CopyFileTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, CopyFileTaskTypeImpl.PARAM_SOURCE_SERVICE, ATT_SOURCE_SERVICE);
        dataUtil.setTaskParameterToAttribute(
                task1, CopyFileTaskTypeImpl.PARAM_TARGET_SERVICE, ATT_TARGET_SERVICE);
        dataUtil.setTaskParameterToAttribute(
                task1, CopyFileTaskTypeImpl.PARAM_SOURCE_PATH, ATT_SOURCE_PATH);
        dataUtil.setTaskParameterToAttribute(
                task1, CopyFileTaskTypeImpl.PARAM_TARGET_PATH, ATT_TARGET_PATH);
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
    public void testSuccessAndCleanup() throws SchedulerException, SQLException, IOException {

        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_SERVICE, SOURCE_SERVICE);
        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_PATH, SOURCE_FILE);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_SERVICE, TARGET_SERVICE);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_PATH, TARGET_FILE_PATTERN);
        config = dao.save(config);

        FileService fileService =
                fileServices.get(S3FileServiceImpl.name(TARGET_ALIAS, TARGET_BUCKET));
        assertTrue(fileService.checkFileExists(TARGET_FILE_OLD));
        assertFalse(fileService.checkFileExists(TARGET_FILE_NEW));

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertFalse(fileService.checkFileExists(TARGET_FILE_OLD));
        assertTrue(fileService.checkFileExists(TARGET_FILE_NEW));

        assertTrue(taskUtil.cleanup(config));

        assertFalse(fileService.checkFileExists(TARGET_FILE_OLD));
        assertFalse(fileService.checkFileExists(TARGET_FILE_NEW));
    }

    @Test
    public void testRollback() throws SchedulerException, SQLException, IOException {
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task2, TestTaskTypeImpl.PARAM_FAIL, "fail");
        dataUtil.addTaskToConfiguration(config, task2);
        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_SERVICE, SOURCE_SERVICE);
        dataUtil.setConfigurationAttribute(config, ATT_SOURCE_PATH, SOURCE_FILE);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_SERVICE, TARGET_SERVICE);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET_PATH, TARGET_FILE_PATTERN);
        dataUtil.setConfigurationAttribute(config, "fail", "true");
        config = dao.save(config);
        task2 = config.getTasks().get("task2");
        dataUtil.addBatchElement(batch, task2);
        batch = bjService.saveAndSchedule(batch);

        FileService fileService =
                fileServices.get(S3FileServiceImpl.name(TARGET_ALIAS, TARGET_BUCKET));
        assertTrue(fileService.checkFileExists(TARGET_FILE_OLD));
        assertFalse(fileService.checkFileExists(TARGET_FILE_NEW));

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertTrue(fileService.checkFileExists(TARGET_FILE_OLD));
        assertFalse(fileService.checkFileExists(TARGET_FILE_NEW));
    }
}
