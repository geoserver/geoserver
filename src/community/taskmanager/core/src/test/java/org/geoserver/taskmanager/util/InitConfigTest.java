package org.geoserver.taskmanager.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.beans.DummyTaskTypeImpl;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class InitConfigTest extends AbstractTaskManagerTest {

    private static final String ATT_DUMMY1 = "dummy1";

    private static final String ATT_DUMMY2 = "dummy2";

    private static final String ATT_DUMMY3 = "dummy3";

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil util;

    @Autowired private TaskManagerTaskUtil taskUtil;

    @Autowired private InitConfigUtil initUtil;

    private Configuration config;

    @Before
    public void setupConfiguration() {
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        util.setConfigurationAttribute(config, ATT_DUMMY1, "foo");
        util.setConfigurationAttribute(config, ATT_DUMMY2, "dummy");

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(DummyTaskTypeImpl.NAME);
        util.setTaskParameterToAttribute(task1, DummyTaskTypeImpl.PARAM1, ATT_DUMMY1);
        util.setTaskParameterToAttribute(task1, DummyTaskTypeImpl.PARAM2, ATT_DUMMY2);
        util.addTaskToConfiguration(config, task1);

        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(DummyTaskTypeImpl.NAME);
        util.setTaskParameterToAttribute(task2, DummyTaskTypeImpl.PARAM1, ATT_DUMMY2);
        util.setTaskParameterToAttribute(task2, DummyTaskTypeImpl.PARAM2, ATT_DUMMY3);
        util.addTaskToConfiguration(config, task2);

        Batch batch = fac.createBatch();
        batch.setName("@Initialize");
        util.addBatchElement(batch, task1);
        util.addBatchToConfiguration(config, batch);

        Batch otherBatch = fac.createBatch();
        otherBatch.setName("otherBatch");
        util.addBatchElement(otherBatch, task1);
        util.addBatchElement(otherBatch, task2);
        util.addBatchToConfiguration(config, otherBatch);

        config = dao.save(config);
    }

    @After
    public void clearDataFromDatabase() {
        dao.delete(config);
    }

    @Test
    public void testInitConfig() {
        assertEquals(config.getBatches().get("@Initialize"), InitConfigUtil.getInitBatch(config));
        assertTrue(initUtil.isInitConfig(config));

        Configuration initConfig = initUtil.wrap(config);
        assertNotEquals(config, initConfig);
        assertEquals(config, InitConfigUtil.unwrap(initConfig));

        assertEquals(config.getId(), initConfig.getId());
        assertEquals(config.getName(), initConfig.getName());

        assertEquals(2, config.getTasks().size());
        assertEquals(1, initConfig.getTasks().size());

        assertEquals(2, config.getBatches().size());
        assertEquals(1, initConfig.getBatches().size());

        Attribute attDummy2 = config.getAttributes().get(ATT_DUMMY2);

        List<Parameter> params = util.getAssociatedParameters(attDummy2, config);
        assertEquals(2, params.size());
        assertEquals(config.getTasks().get("task1"), params.get(0).getTask());
        assertEquals(config.getTasks().get("task2"), params.get(1).getTask());
        params = util.getAssociatedParameters(attDummy2, initConfig);
        assertEquals(1, params.size());
        assertEquals(config.getTasks().get("task1"), params.get(0).getTask());

        assertFalse(taskUtil.validate(config).isEmpty());
        assertTrue(taskUtil.validate(initConfig).isEmpty());

        assertEquals(1, taskUtil.getActionsForAttribute(attDummy2, config).size());
        assertEquals(1, taskUtil.getActionsForAttribute(attDummy2, initConfig).size());

        assertEquals(2, taskUtil.getTypesForAttribute(attDummy2, config).size());
        assertEquals(1, taskUtil.getTypesForAttribute(attDummy2, initConfig).size());
    }
}
