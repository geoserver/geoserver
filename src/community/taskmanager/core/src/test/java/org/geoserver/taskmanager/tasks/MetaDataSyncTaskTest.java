/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.decoder.RESTCoverage;
import java.net.MalformedURLException;
import java.sql.SQLException;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
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
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * To run this test you should have a geoserver running on http://localhost:9090/geoserver.
 *
 * @author Niels Charlier
 */
public class MetaDataSyncTaskTest extends AbstractTaskManagerTest {

    private static final String ATT_LAYER = "layer";
    static final String ATT_EXT_GS = "geoserver";

    @Autowired private LookupService<ExternalGS> extGeoservers;

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    private Configuration config;

    private Batch batchCreate;

    private Batch batchSync;

    @Override
    public boolean setupDataDirectory() throws Exception {
        DATA_DIRECTORY.addWcs11Coverages();
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
        dataUtil.addTaskToConfiguration(config, task1);

        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(MetadataSyncTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task2, MetadataSyncTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.setTaskParameterToAttribute(
                task2, MetadataSyncTaskTypeImpl.PARAM_EXT_GS, ATT_EXT_GS);
        dataUtil.addTaskToConfiguration(config, task2);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");

        batchCreate = fac.createBatch();
        batchCreate.setName("batchCreate");
        dataUtil.addBatchElement(batchCreate, task1);

        batchSync = fac.createBatch();
        batchSync.setName("batchSync");
        dataUtil.addBatchElement(batchSync, task2);

        batchCreate = bjService.saveAndSchedule(batchCreate);
        batchSync = bjService.saveAndSchedule(batchSync);
    }

    @After
    public void clearDataFromDatabase() {
        if (batchCreate != null) {
            dao.delete(batchCreate);
        }
        if (batchSync != null) {
            dao.delete(batchSync);
        }
        if (config != null) {
            dao.delete(config);
        }
    }

    @Test
    public void test() throws SchedulerException, SQLException, MalformedURLException {
        // set some metadata
        CoverageInfo ci = geoServer.getCatalog().getCoverageByName("DEM");
        ci.setTitle("original title");
        ci.setAbstract("original abstract");
        geoServer.getCatalog().save(ci);

        dataUtil.setConfigurationAttribute(config, ATT_LAYER, "DEM");
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger()
                        .forJob(batchCreate.getId().toString())
                        .startNow()
                        .build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertTrue(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertTrue(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));
        assertTrue(restManager.getReader().existsLayer("wcs", "DEM", true));

        RESTCoverage cov = restManager.getReader().getCoverage("wcs", "DEM", "DEM");
        assertEquals(ci.getTitle(), cov.getTitle());
        assertEquals(ci.getAbstract(), cov.getAbstract());
        assertEquals(
                ci.getDimensions().get(0).getName(),
                cov.getEncodedDimensionsInfoList().get(0).getName());

        // metadata sync
        ci.setTitle("new title");
        ci.setAbstract("new abstract");
        ci.getDimensions().get(0).setName("CUSTOM_DIMENSION");
        ci.getMetadata().put("something", "anything");
        geoServer.getCatalog().save(ci);

        trigger =
                TriggerBuilder.newTrigger().forJob(batchSync.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        cov = restManager.getReader().getCoverage("wcs", "DEM", "DEM");
        assertEquals(ci.getTitle(), cov.getTitle());
        assertEquals(ci.getAbstract(), cov.getAbstract());
        assertEquals(
                ci.getDimensions().get(0).getName(),
                cov.getEncodedDimensionsInfoList().get(0).getName());
        assertEquals("something", cov.getMetadataList().get(0).getKey());
        assertEquals("anything", cov.getMetadataList().get(0).getMetadataElem().getText());

        // clean-up

        assertTrue(taskUtil.cleanup(config));

        assertFalse(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertFalse(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));
        assertFalse(restManager.getReader().existsLayer("wcs", "DEM", true));
    }
}
