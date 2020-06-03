/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.decoder.RESTCoverage;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.metadata.data.model.impl.MetadataTemplateImpl;
import org.geoserver.metadata.data.service.MetadataTemplateService;
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
public class MetaDataTemplateSyncTaskTest extends AbstractTaskManagerTest {

    /** If your target geoserver supports the metadata module. */
    private static final boolean SUPPORTS_METADATA = false;

    private static final String ATT_LAYER = "layer";
    private static final String ATT_EXT_GS = "geoserver";
    private static final String ATT_TEMPLATE_NAME = "template-name";
    private static final String TEMPLATE_NAME = "myTemplate";

    @Autowired private LookupService<ExternalGS> extGeoservers;

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    @Autowired private MetadataTemplateService templateService;

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
        task2.setType(MetadataTemplateSyncTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task2, MetadataTemplateSyncTaskTypeImpl.PARAM_METADATA_TEMPLATE, ATT_TEMPLATE_NAME);
        dataUtil.setTaskParameterToAttribute(
                task2, MetadataTemplateSyncTaskTypeImpl.PARAM_EXT_GS, ATT_EXT_GS);
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

        config = dao.init(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");
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
    public void test() throws SchedulerException, SQLException, IOException {
        // create template
        MetadataTemplateImpl template = new MetadataTemplateImpl();
        template.setId(UUID.randomUUID().toString());
        template.setName(TEMPLATE_NAME);
        template.getLinkedLayers().add(geoServer.getCatalog().getCoverageByName("DEM").getId());
        template.getLinkedLayers().add(geoServer.getCatalog().getCoverageByName("World").getId());
        templateService.save(template);

        // set some metadata
        CoverageInfo ci = geoServer.getCatalog().getCoverageByName("DEM");
        ci.setTitle("original title");
        ci.setAbstract("original abstract");
        geoServer.getCatalog().save(ci);

        dataUtil.setConfigurationAttribute(config, ATT_LAYER, "DEM");
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        dataUtil.setConfigurationAttribute(config, ATT_TEMPLATE_NAME, TEMPLATE_NAME);
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

        RESTLayer layer = restManager.getReader().getLayer("wcs", "DEM");
        assertNotNull(layer);

        // metadata sync
        ci.setTitle("new title");
        ci.setAbstract("new abstract");
        ci.getDimensions().get(0).setName("CUSTOM_DIMENSION");
        ci.getMetadata().put("asomething", "anything");
        ci.getMetadata().put("adate", new Date());
        if (SUPPORTS_METADATA) {
            HashMap<String, String> map = new HashMap<>();
            map.put("foo", "bar");
            map.put("boo", "far");
            ci.getMetadata().put("complex", map);
        }
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
        assertEquals("asomething", cov.getMetadataList().get(0).getKey());
        assertEquals("anything", cov.getMetadataList().get(0).getMetadataElem().getText());
        assertEquals("adate", cov.getMetadataList().get(1).getKey());
        assertNotNull(cov.getMetadataList().get(1).getMetadataElem().getChild("date"));
        if (SUPPORTS_METADATA) {
            assertEquals("complex", cov.getMetadataList().get(2).getKey());
            assertNotNull(cov.getMetadataList().get(2).getMetadataElem().getChild("map"));
        }

        assertEquals("The following layers failed to synchronize: wcs:World", getSuccessMessage());

        // clean-up

        assertTrue(taskUtil.cleanup(config));

        assertFalse(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertFalse(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));
        assertFalse(restManager.getReader().existsLayer("wcs", "DEM", true));
    }

    public String getSuccessMessage() {
        batchSync = dao.initHistory(batchSync);
        return batchSync.getBatchRuns().get(0).getRuns().get(0).getMessage();
    }
}
