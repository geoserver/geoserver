/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.data.CatalogWriter;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.external.ExternalGS;
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
public class FileRemoteS3PublicationTaskTest extends AbstractTaskManagerTest {

    private static final Logger LOGGER = Logging.getLogger(FileRemoteS3PublicationTaskTest.class);

    // configure these constants
    private static QName REMOTE_COVERAGE = new QName("gs", "mylayer", "gs");
    private static String REMOTE_COVERAGE_ALIAS = "test";
    private static String REMOTE_COVERAGE_BUCKET = "source";
    private static String REMOTE_COVERAGE_FILE_LOCATION = "test/salinity.tif";
    private static String REMOTE_COVERAGE_URL =
            REMOTE_COVERAGE_ALIAS
                    + "://"
                    + REMOTE_COVERAGE_BUCKET
                    + "/"
                    + REMOTE_COVERAGE_FILE_LOCATION;
    private static String REMOTE_COVERAGE_OTHER_BUCKET = "target";
    private static String REMOTE_COVERAGE_OTHER_FILE_LOCATION_PATTERN = "test/salinity.###.tif";
    private static String REMOTE_COVERAGE_OTHER_FILE_LOCATION_OLD = "test/salinity.41.tif";
    private static String REMOTE_COVERAGE_OTHER_FILE_LOCATION = "test/salinity.42.tif";
    private static String REMOTE_COVERAGE_OTHER_URL =
            REMOTE_COVERAGE_ALIAS
                    + "://"
                    + REMOTE_COVERAGE_OTHER_BUCKET
                    + "/"
                    + REMOTE_COVERAGE_OTHER_FILE_LOCATION;
    private static String REMOTE_COVERAGE_TYPE = "S3GeoTiff";

    private static final String ATT_LAYER = "layer";
    private static final String ATT_EXT_GS = "geoserver";
    private static final String ATT_FILE_SERVICE = "fileService";
    private static final String ATT_FILE = "file";

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
        try {
            FileService fileService =
                    fileServices.get(
                            S3FileServiceImpl.name(REMOTE_COVERAGE_ALIAS, REMOTE_COVERAGE_BUCKET));
            Assume.assumeNotNull(fileService);
            Assume.assumeTrue(
                    "File exists on s3 service",
                    fileService.checkFileExists(REMOTE_COVERAGE_FILE_LOCATION));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            Assume.assumeTrue("S3 service is configured and available", false);
        }

        DATA_DIRECTORY.addWcs11Coverages();
        Map<String, String> params = new HashMap<String, String>();
        params.put(CatalogWriter.COVERAGE_TYPE_KEY, REMOTE_COVERAGE_TYPE);
        params.put(CatalogWriter.COVERAGE_URL_KEY, REMOTE_COVERAGE_URL);
        DATA_DIRECTORY.addCustomCoverage(REMOTE_COVERAGE, params);
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
        task1.setType(FileRemotePublicationTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task1, FileRemotePublicationTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.setTaskParameterToAttribute(
                task1, FileRemotePublicationTaskTypeImpl.PARAM_EXT_GS, ATT_EXT_GS);
        dataUtil.setTaskParameterToAttribute(
                task1, FileRemotePublicationTaskTypeImpl.PARAM_FILE, ATT_FILE);
        dataUtil.setTaskParameterToAttribute(
                task1, FileRemotePublicationTaskTypeImpl.PARAM_FILE_SERVICE, ATT_FILE_SERVICE);
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
    public void testS3SuccessAndCleanup()
            throws SchedulerException, SQLException, MalformedURLException {
        dataUtil.setConfigurationAttribute(
                config,
                ATT_LAYER,
                REMOTE_COVERAGE.getPrefix() + ":" + REMOTE_COVERAGE.getLocalPart());
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertTrue(
                restManager
                        .getReader()
                        .existsCoveragestore(
                                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart()));
        assertTrue(
                restManager
                        .getReader()
                        .existsCoverage(
                                REMOTE_COVERAGE.getPrefix(),
                                REMOTE_COVERAGE.getLocalPart(),
                                REMOTE_COVERAGE.getLocalPart()));
        assertTrue(
                restManager
                        .getReader()
                        .existsLayer(
                                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart(), true));

        assertTrue(taskUtil.cleanup(config));

        assertFalse(
                restManager
                        .getReader()
                        .existsCoveragestore(
                                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart()));
        assertFalse(
                restManager
                        .getReader()
                        .existsCoverage(
                                REMOTE_COVERAGE.getPrefix(),
                                REMOTE_COVERAGE.getLocalPart(),
                                REMOTE_COVERAGE.getLocalPart()));
        assertFalse(
                restManager
                        .getReader()
                        .existsLayer(
                                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart(), true));
    }

    @Test
    public void testS3ReplaceUrlSuccessAndCleanup()
            throws SchedulerException, SQLException, IOException {
        FileService otherFileService = null;
        try {
            FileService fileService =
                    fileServices.get(
                            S3FileServiceImpl.name(REMOTE_COVERAGE_ALIAS, REMOTE_COVERAGE_BUCKET));
            otherFileService =
                    fileServices.get(
                            S3FileServiceImpl.name(
                                    REMOTE_COVERAGE_ALIAS, REMOTE_COVERAGE_OTHER_BUCKET));
            Assume.assumeNotNull(otherFileService);
            try (InputStream is = fileService.read(REMOTE_COVERAGE_FILE_LOCATION)) {
                otherFileService.create(REMOTE_COVERAGE_OTHER_FILE_LOCATION_OLD, is);
            }
            try (InputStream is = fileService.read(REMOTE_COVERAGE_FILE_LOCATION)) {
                otherFileService.create(REMOTE_COVERAGE_OTHER_FILE_LOCATION, is);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            Assume.assumeTrue("S3 service is configured and available", false);
        }

        dataUtil.setConfigurationAttribute(
                config,
                ATT_LAYER,
                REMOTE_COVERAGE.getPrefix() + ":" + REMOTE_COVERAGE.getLocalPart());
        dataUtil.setConfigurationAttribute(
                config,
                ATT_FILE_SERVICE,
                S3FileServiceImpl.name(REMOTE_COVERAGE_ALIAS, REMOTE_COVERAGE_OTHER_BUCKET));
        dataUtil.setConfigurationAttribute(
                config, ATT_FILE, REMOTE_COVERAGE_OTHER_FILE_LOCATION_PATTERN);
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertTrue(
                restManager
                        .getReader()
                        .existsCoveragestore(
                                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart()));
        assertEquals(
                REMOTE_COVERAGE_OTHER_URL,
                restManager
                        .getReader()
                        .getCoverageStore(
                                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart())
                        .getURL());
        assertTrue(
                restManager
                        .getReader()
                        .existsCoverage(
                                REMOTE_COVERAGE.getPrefix(),
                                REMOTE_COVERAGE.getLocalPart(),
                                REMOTE_COVERAGE.getLocalPart()));
        assertTrue(
                restManager
                        .getReader()
                        .existsLayer(
                                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart(), true));

        assertTrue(taskUtil.cleanup(config));

        assertFalse(
                restManager
                        .getReader()
                        .existsCoveragestore(
                                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart()));
        assertFalse(
                restManager
                        .getReader()
                        .existsCoverage(
                                REMOTE_COVERAGE.getPrefix(),
                                REMOTE_COVERAGE.getLocalPart(),
                                REMOTE_COVERAGE.getLocalPart()));
        assertFalse(
                restManager
                        .getReader()
                        .existsLayer(
                                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart(), true));

        otherFileService.delete(REMOTE_COVERAGE_OTHER_FILE_LOCATION);
        otherFileService.delete(REMOTE_COVERAGE_OTHER_FILE_LOCATION_OLD);
    }
}
