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
import it.geosolutions.geoserver.rest.decoder.RESTCoverage;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.HashMap;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
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
import org.geoserver.util.IOUtils;
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

    /** If your target geoserver supports the metadata module. */
    private static final boolean SUPPORTS_METADATA = true;

    private static final String STYLE = "grass";
    private static final String SECOND_STYLE = "second_grass";
    private static final String ATT_LAYER = "layer";
    static final String ATT_EXT_GS = "geoserver";

    @Autowired private LookupService<ExternalGS> extGeoservers;

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    @Autowired private GeoServerDataDirectory dd;

    private Configuration config;

    private Batch batchCreate;

    private Batch batchSync;

    @Override
    public boolean setupDataDirectory() throws Exception {
        DATA_DIRECTORY.addStyle(STYLE, getClass().getResource(STYLE + ".sld"));
        DATA_DIRECTORY.addStyle(SECOND_STYLE, getClass().getResource(SECOND_STYLE + ".sld"));
        try (InputStream is = getClass().getResource("grass_fill.png").openStream()) {
            try (OutputStream os =
                    new FileOutputStream(
                            new File(
                                    DATA_DIRECTORY.getDataDirectoryRoot(),
                                    "styles/grass_fill.png"))) {
                IOUtils.copy(is, os);
            }
        }
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
        // set some metadata
        CoverageInfo ci = geoServer.getCatalog().getCoverageByName("DEM");
        ci.setTitle("original title");
        ci.setAbstract("original abstract");
        geoServer.getCatalog().save(ci);
        // set a style
        LayerInfo li = geoServer.getCatalog().getLayerByName("DEM");
        StyleInfo si = geoServer.getCatalog().getStyleByName(STYLE);
        li.setDefaultStyle(si);
        geoServer.getCatalog().save(li);

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

        RESTLayer layer = restManager.getReader().getLayer("wcs", "DEM");
        assertEquals(STYLE, layer.getDefaultStyle());
        assertNull(layer.getStyles());

        // metadata sync
        ci.setTitle("new title");
        ci.setAbstract("new abstract");
        ci.getDimensions().get(0).setName("CUSTOM_DIMENSION");
        ci.getMetadata().put("asomething", "anything");
        if (SUPPORTS_METADATA) {
            HashMap<String, String> map = new HashMap<>();
            map.put("foo", "bar");
            map.put("boo", "far");
            ci.getMetadata().put("complex", map);
        }
        geoServer.getCatalog().save(ci);
        li.getStyles().add(geoServer.getCatalog().getStyleByName(SECOND_STYLE));
        geoServer.getCatalog().save(li);
        try (OutputStream out = dd.style(si).out()) {
            try (InputStream in = getClass().getResource("third_grass.sld").openStream()) {
                IOUtils.copy(in, out);
            }
        }

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
        if (SUPPORTS_METADATA) {
            assertEquals("complex", cov.getMetadataList().get(1).getKey());
            assertNotNull(cov.getMetadataList().get(1).getMetadataElem().getChild("map"));
        }
        layer = restManager.getReader().getLayer("wcs", "DEM");
        assertEquals(STYLE, layer.getDefaultStyle());
        assertEquals(1, layer.getStyles().size());
        assertEquals(SECOND_STYLE, layer.getStyles().get(0).getName());

        String style = restManager.getStyleManager().getSLD(STYLE);
        assertTrue(style.indexOf("CHANGED VERSION") > 0);

        // clean-up

        assertTrue(taskUtil.cleanup(config));

        assertFalse(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertFalse(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));
        assertFalse(restManager.getReader().existsLayer("wcs", "DEM", true));
    }
}
