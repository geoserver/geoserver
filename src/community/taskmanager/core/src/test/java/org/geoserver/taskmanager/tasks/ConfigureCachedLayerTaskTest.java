package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.encoder.GSCachedLayerEncoder;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Collections;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
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
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.filter.parameters.IntegerParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;
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

public class ConfigureCachedLayerTaskTest extends AbstractTaskManagerTest {

    private static final String ATT_LAYER = "layer";
    private static final String ATT_EXT_GS = "geoserver";
    private static final String ATT_FILE = "file";

    @Autowired private LookupService<ExternalGS> extGeoservers;

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    private Configuration config;

    private Batch batch;

    private Batch batchUpdate;

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
        dataUtil.setTaskParameterToAttribute(
                task1, FileRemotePublicationTaskTypeImpl.PARAM_FILE, ATT_FILE);
        dataUtil.addTaskToConfiguration(config, task1);

        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(ConfigureCachedLayerTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(
                task2, ConfigureCachedLayerTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.setTaskParameterToAttribute(
                task2, ConfigureCachedLayerTaskTypeImpl.PARAM_EXT_GS, ATT_EXT_GS);
        dataUtil.addTaskToConfiguration(config, task2);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");

        batch = fac.createBatch();

        batch.setName("my_batch");
        dataUtil.addBatchElement(batch, task1);
        dataUtil.addBatchElement(batch, task2);

        batch = bjService.saveAndSchedule(batch);

        batchUpdate = fac.createBatch();

        batchUpdate.setName("batchUpdate");
        dataUtil.addBatchElement(batchUpdate, task2);

        batchUpdate = bjService.saveAndSchedule(batchUpdate);

        config = dao.init(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");
    }

    @After
    public void clearDataFromDatabase() {
        if (batch != null) {
            dao.delete(batch);
        }
        if (batchUpdate != null) {
            dao.delete(batchUpdate);
        }
        if (config != null) {
            dao.delete(config);
        }
    }

    @Test
    public void testConfigureAndDelete()
            throws SchedulerException, SQLException, MalformedURLException {
        // configure caching
        GWC gwc = GWC.get();
        final GeoServerTileLayer tileLayer =
                new GeoServerTileLayer(
                        gwc.getLayerInfoByName("DEM"), gwc.getConfig(), gwc.getGridSetBroker());
        tileLayer.getInfo().setEnabled(true);
        tileLayer.getInfo().setExpireCache(20);
        tileLayer.getInfo().setExpireClients(30);
        tileLayer.getInfo().setGutter(7);
        tileLayer.getInfo().setBlobStoreId("myblob");
        tileLayer.getInfo().setMetaTilingX(1);
        tileLayer.getInfo().setMetaTilingY(2);
        ParameterFilter a = new IntegerParameterFilter();
        a.setKey("a");
        a.setDefaultValue("1");
        tileLayer.getInfo().addParameterFilter(a);
        ParameterFilter b = new IntegerParameterFilter();
        b.setKey("b");
        b.setDefaultValue("2");
        tileLayer.getInfo().addParameterFilter(b);
        XMLGridSubset gridSubset = new XMLGridSubset();
        gridSubset.setGridSetName("MyGridSubSet");
        gridSubset.setZoomStart(1);
        tileLayer.getInfo().getGridSubsets().clear();
        tileLayer.getInfo().getGridSubsets().add(gridSubset);
        tileLayer.getInfo().getMimeFormats().clear();
        tileLayer.getInfo().getMimeFormats().add("my/mime");
        tileLayer.getInfo().getMimeFormats().add("my/dime");
        gwc.add(tileLayer);

        dataUtil.setConfigurationAttribute(config, ATT_LAYER, "DEM");
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertTrue(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertTrue(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));
        assertTrue(restManager.getReader().existsLayer("wcs", "DEM", true));

        GSCachedLayerEncoder enc = restManager.getGeoWebCacheRest().getLayer("wcs:DEM");
        assertNotNull(enc);
        assertEquals(tileLayer.getInfo().getBlobStoreId(), enc.getBlobStoreId());
        assertEquals(tileLayer.getInfo().getExpireCache(), enc.getExpireCache());
        assertEquals(tileLayer.getInfo().getExpireClients(), enc.getExpireClients());
        assertEquals(tileLayer.getInfo().getGutter(), enc.getGutter());

        // delete caching configuration
        gwc.removeTileLayers(Collections.singletonList("wcs:DEM"));

        trigger =
                TriggerBuilder.newTrigger()
                        .forJob(batchUpdate.getId().toString())
                        .startNow()
                        .build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        assertNull(null, restManager.getGeoWebCacheRest().getLayer("wcs:DEM"));

        // clean-up layer
        assertTrue(taskUtil.cleanup(config));

        assertFalse(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertFalse(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));
        assertFalse(restManager.getReader().existsLayer("wcs", "DEM", true));
    }
}
