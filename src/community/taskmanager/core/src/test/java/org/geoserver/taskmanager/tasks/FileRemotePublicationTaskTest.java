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
import it.geosolutions.geoserver.rest.decoder.RESTFeatureType;
import it.geosolutions.geoserver.rest.decoder.RESTLayer;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.util.LookupService;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.geotools.data.complex.AppSchemaDataAccessFactory;
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
public class FileRemotePublicationTaskTest extends AbstractTaskManagerTest {

    private static final String ATT_LAYER = "layer";
    private static final String ATT_EXT_GS = "geoserver";
    private static final String ATT_FAIL = "fail";
    private static final String ATT_FILE = "file";

    @Autowired private LookupService<ExternalGS> extGeoservers;

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil dataUtil;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private BatchJobService bjService;

    @Autowired private Scheduler scheduler;

    @Autowired private LookupService<FileService> fileServices;

    @Autowired private ExternalGS externalGS;

    private Configuration config;

    private Batch batch;

    @Override
    public boolean setupDataDirectory() throws Exception {
        DATA_DIRECTORY.addWcs11Coverages();
        Map<String, Serializable> params = new HashMap<>();

        FileService fileService = fileServices.get("data-directory");
        if (!fileService.checkFileExists("MappedFeature.xml")) {
            try (InputStream in =
                    getClass().getResource("appschema/MappedFeature.xml").openStream()) {
                fileService.create("MappedFeature.xml", in);
            }
        }
        try (InputStream in =
                getClass().getResource("appschema/MappedFeature.properties").openStream()) {
            externalGS
                    .getRESTManager()
                    .getResourceManager()
                    .upload("uploaded-stores/MappedFeature.properties", in);
        }

        params.put(AppSchemaDataAccessFactory.URL.key, "file:data/MappedFeature.xml");
        params.put(AppSchemaDataAccessFactory.DBTYPE.key, AppSchemaDataAccessFactory.DBTYPE_STRING);
        DATA_DIRECTORY.addCustomType(
                new QName("urn:cgi:xmlns:CGI:GeoSciML:2.0", "MappedFeature", "gsml"), params);

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
        if (batch != null) {
            dao.delete(batch);
        }
        if (config != null) {
            dao.delete(config);
        }
    }

    @Test
    public void testRasterSuccessAndCleanup()
            throws SchedulerException, SQLException, MalformedURLException {
        // set some metadata
        CoverageInfo ci = geoServer.getCatalog().getCoverageByName("DEM");
        ci.setName("mydem");
        ci.setTitle("my title ë");
        ci.setAbstract("my abstract ë");
        ci.getDimensions().get(0).setName("CUSTOM_DIMENSION");
        ci.getKeywords().add(new Keyword("demmiedem"));
        geoServer.getCatalog().save(ci);

        LayerInfo li = geoServer.getCatalog().getLayerByName("mydem");
        LayerIdentifier lid1 = new LayerIdentifier();
        lid1.setAuthority("auth1");
        lid1.setIdentifier("id1");
        li.getIdentifiers().add(lid1);
        LayerIdentifier lid2 = new LayerIdentifier();
        lid2.setAuthority("auth2");
        lid2.setIdentifier("id2");
        li.getIdentifiers().add(lid2);
        AuthorityURL url1 = new AuthorityURL();
        url1.setName("name1");
        url1.setHref("href1");
        li.getAuthorityURLs().add(url1);
        AuthorityURL url2 = new AuthorityURL();
        url2.setName("name2");
        url2.setHref("href2");
        li.getAuthorityURLs().add(url2);
        geoServer.getCatalog().save(li);

        dataUtil.setConfigurationAttribute(config, ATT_LAYER, "mydem");
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertTrue(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertTrue(restManager.getReader().existsCoverage("wcs", "DEM", "mydem"));
        assertTrue(restManager.getReader().existsLayer("wcs", "mydem", true));
        assertFalse(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));

        RESTCoverage cov = restManager.getReader().getCoverage("wcs", "DEM", "mydem");
        assertEquals(ci.getTitle(), cov.getTitle());
        assertEquals(ci.getAbstract(), cov.getAbstract());
        assertEquals(
                ci.getDimensions().get(0).getName(),
                cov.getEncodedDimensionsInfoList().get(0).getName());
        assertTrue(cov.getKeywords().contains("demmiedem"));

        RESTLayer layer = restManager.getReader().getLayer("wcs", "mydem");
        assertEquals(2, layer.getEncodedAuthorityURLInfoList().size());
        assertEquals("name1", layer.getEncodedAuthorityURLInfoList().get(0).getName());
        assertEquals("href1", layer.getEncodedAuthorityURLInfoList().get(0).getHref());
        assertEquals("name2", layer.getEncodedAuthorityURLInfoList().get(1).getName());
        assertEquals("href2", layer.getEncodedAuthorityURLInfoList().get(1).getHref());
        assertEquals(2, layer.getEncodedIdentifierInfoList().size());
        assertEquals("auth1", layer.getEncodedIdentifierInfoList().get(0).getAuthority());
        assertEquals("id1", layer.getEncodedIdentifierInfoList().get(0).getIdentifier());
        assertEquals("auth2", layer.getEncodedIdentifierInfoList().get(1).getAuthority());
        assertEquals("id2", layer.getEncodedIdentifierInfoList().get(1).getIdentifier());

        assertTrue(taskUtil.cleanup(config));

        assertFalse(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertFalse(restManager.getReader().existsCoverage("wcs", "DEM", "mydem"));
        assertFalse(restManager.getReader().existsLayer("wcs", "mydem", true));

        // restore name
        ci.setName("DEM");
        geoServer.getCatalog().save(ci);
    }

    @Test
    public void testVectorSuccessAndCleanup()
            throws SchedulerException, SQLException, MalformedURLException {
        // set some metadata
        FeatureTypeInfo fi = geoServer.getCatalog().getFeatureTypeByName("MappedFeature");
        fi.setTitle("my title ë");
        fi.setAbstract("my abstract ë");
        geoServer.getCatalog().save(fi);

        dataUtil.setConfigurationAttribute(config, ATT_LAYER, "MappedFeature");
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        config = dao.save(config);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertTrue(restManager.getReader().existsDatastore("gsml", "gsml"));
        assertTrue(restManager.getReader().existsFeatureType("gsml", "gsml", "MappedFeature"));
        assertTrue(restManager.getReader().existsLayer("gsml", "MappedFeature", true));

        RESTLayer layer = restManager.getReader().getLayer("gsml", "MappedFeature");
        RESTFeatureType ft = restManager.getReader().getFeatureType(layer);
        assertEquals(fi.getTitle(), ft.getTitle());
        assertEquals(fi.getAbstract(), ft.getAbstract());

        assertTrue(taskUtil.cleanup(config));

        assertFalse(restManager.getReader().existsDatastore("gsml", "gsml"));
        assertFalse(restManager.getReader().existsFeatureType("gsml", "gsml", "MappedFeature"));
        assertFalse(restManager.getReader().existsLayer("gsml", "MappedFeature", true));
    }

    @Test
    public void testRollback() throws SchedulerException, SQLException, MalformedURLException {
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task2, TestTaskTypeImpl.PARAM_FAIL, ATT_FAIL);
        dataUtil.addTaskToConfiguration(config, task2);

        dataUtil.setConfigurationAttribute(config, ATT_LAYER, "DEM");
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        dataUtil.setConfigurationAttribute(config, ATT_FAIL, Boolean.TRUE.toString());
        config = dao.save(config);
        task2 = config.getTasks().get("task2");
        dataUtil.addBatchElement(batch, task2);
        batch = bjService.saveAndSchedule(batch);

        Trigger trigger =
                TriggerBuilder.newTrigger().forJob(batch.getId().toString()).startNow().build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertFalse(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertFalse(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));
        assertFalse(restManager.getReader().existsLayer("wcs", "DEM", true));
    }
}
