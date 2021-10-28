/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.decoder.RESTDataRules;
import it.geosolutions.geoserver.rest.manager.GeoServerRESTSecurityManager.RuleType;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class LayerSecuritySyncTaskTest extends AbstractTaskManagerTest {

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

    @Autowired protected DataAccessRuleDAO dataAccessDao;

    private Configuration config;

    private Batch batch;

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
        task2.setType(LayerSecuritySyncTaskTypeImpl.NAME);
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
    public void testSyncAndCleanup() throws SchedulerException, SQLException, IOException {
        // run with admin rights
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                "admin",
                                null,
                                Collections.singletonList(GeoServerRole.ADMIN_ROLE)));

        // configure security
        dataAccessDao.addRule(
                new DataAccessRule("wcs", "DEM", AccessMode.READ, Collections.emptySet()));
        dataAccessDao.addRule(
                new DataAccessRule(
                        "wcs", "DEM", AccessMode.WRITE, Sets.newHashSet("ROLE_1", "ROLE_2")));
        dataAccessDao.storeRules();

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

        RESTDataRules dataRules = restManager.getSecurityManager().getDataRules();
        assertTrue(dataRules.getRule("wcs", "DEM", RuleType.R).isEmpty());
        assertEquals(
                Sets.newHashSet("ROLE_1", "ROLE_2"), dataRules.getRule("wcs", "DEM", RuleType.W));

        // clean-up layer
        assertTrue(taskUtil.cleanup(config));

        assertFalse(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertFalse(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));
        assertFalse(restManager.getReader().existsLayer("wcs", "DEM", true));
        dataRules = restManager.getSecurityManager().getDataRules();
        assertNull(dataRules.getRule("wcs", "DEM", RuleType.R));
        assertNull(dataRules.getRule("wcs", "DEM", RuleType.W));
    }
}
