/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.beans.DummyAction;
import org.geoserver.taskmanager.beans.DummyTaskTypeImpl;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.util.ValidationError.ValidationErrorType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** @author Niels Charlier */
public class TaskManagerTaskUtilTest extends AbstractTaskManagerTest {

    private static final String ATT_DELAY = "delay";

    private static final String ATT_FAIL = "fail";

    private static final String ATT_DUMMY = "dummy";

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil util;

    @Autowired private TaskManagerTaskUtil taskUtil;

    private Configuration config;

    private Task task1;

    private Task task2;

    @Before
    public void setupConfig() {
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");
        util.setConfigurationAttribute(config, ATT_FAIL, "false");
        util.setConfigurationAttribute(config, ATT_DELAY, "0");
        util.setConfigurationAttribute(config, ATT_DUMMY, "dummy");

        task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(TestTaskTypeImpl.NAME);
        util.setTaskParameterToAttribute(task1, TestTaskTypeImpl.PARAM_FAIL, ATT_FAIL);
        util.setTaskParameterToAttribute(task1, TestTaskTypeImpl.PARAM_DELAY, ATT_DELAY);
        util.addTaskToConfiguration(config, task1);

        task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(DummyTaskTypeImpl.NAME);
        util.setTaskParameterToAttribute(task2, DummyTaskTypeImpl.PARAM1, ATT_FAIL);
        util.setTaskParameterToAttribute(task2, DummyTaskTypeImpl.PARAM2, ATT_DUMMY);
        util.addTaskToConfiguration(config, task2);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");
    }

    @After
    public void cleanUp() {
        dao.delete(config);
    }

    @Test
    public void testCreateContext() throws TaskException {
        TaskContext context = taskUtil.createContext(task1);
        assertEquals(task1, context.getTask());
        assertNotNull(context.getParameterValues());
        assertNull(context.getBatchContext());

        BatchContext bContext = taskUtil.createContext(fac.createBatchRun());
        bContext.put("foo", "bar");
        context = taskUtil.createContext(task1, bContext);
        assertEquals(task1, context.getTask());
        assertNotNull(context.getParameterValues());
        assertNotNull(context.getBatchContext());
        assertNotNull(context.getBatchContext().getBatchRun());
        assertEquals("bar", context.getBatchContext().get("foo"));
    }

    @Test
    public void testBatchContext() throws TaskException {
        final AtomicInteger counter = new AtomicInteger(0);
        BatchContext bContext = taskUtil.createContext(fac.createBatchRun());
        bContext.put("foo", "bar");
        bContext.get(
                "foo",
                new BatchContext.Dependency() {
                    @Override
                    public void revert() throws TaskException {
                        counter.set(counter.get() + 1);
                    }
                });
        bContext.get(
                "foo",
                new BatchContext.Dependency() {
                    @Override
                    public void revert() throws TaskException {
                        counter.set(counter.get() + 2);
                    }
                });
        bContext.delete("foo");
        assertEquals(3, counter.get());
    }

    @Test
    public void testGetActionsForAttribute() {
        List<String> actions =
                taskUtil.getActionsForAttribute(config.getAttributes().get(ATT_FAIL), config);
        assertEquals(1, actions.size());
        assertEquals(DummyAction.NAME, actions.get(0));
    }

    @Test
    public void testGetAndUpdateDomains() {
        Map<String, List<String>> domains = taskUtil.getDomains(config);
        assertEquals(3, domains.size());
        assertEquals(Lists.newArrayList("true"), domains.get(ATT_FAIL));
        assertEquals(Lists.newArrayList("dummy"), domains.get(ATT_DUMMY));

        util.setConfigurationAttribute(config, ATT_FAIL, "true");

        taskUtil.updateDependentDomains(config.getAttributes().get(ATT_FAIL), config, domains);
        assertEquals(Lists.newArrayList("crash", "test", "dummy"), domains.get(ATT_DUMMY));

        util.setConfigurationAttribute(config, ATT_FAIL, "false");

        taskUtil.updateDomains(config, domains, Collections.singleton(ATT_DUMMY));
        assertEquals(Lists.newArrayList("dummy"), domains.get(ATT_DUMMY));
    }

    @Test
    public void testGetParameterValues() throws TaskException {
        Map<String, Object> map = taskUtil.getParameterValues(task1);
        assertTrue(map.get(TestTaskTypeImpl.PARAM_FAIL) instanceof Boolean);
        assertTrue(map.get(TestTaskTypeImpl.PARAM_DELAY) instanceof Integer);
    }

    @Test
    public void testGetTypesForAttribute() {
        Set<ParameterType> types =
                taskUtil.getTypesForAttribute(config.getAttributes().get(ATT_FAIL), config);
        assertEquals(2, types.size());
        assertTrue(types.contains(ParameterType.BOOLEAN));
    }

    @Test
    public void testInitTask() {
        Task t = taskUtil.initTask("Dummy", "newDummyTask");
        assertEquals("newDummyTask", t.getName());
        assertEquals(2, t.getParameters().size());
        assertEquals("${param1}", t.getParameters().get(DummyTaskTypeImpl.PARAM1).getValue());
        assertEquals("${param2}", t.getParameters().get(DummyTaskTypeImpl.PARAM2).getValue());
    }

    @Test
    public void testFixTask() {
        Task t = taskUtil.initTask("Dummy", "newDummyTask");
        t.getParameters().remove(DummyTaskTypeImpl.PARAM2);
        assertEquals(1, t.getParameters().size());
        assertEquals("${param1}", t.getParameters().get(DummyTaskTypeImpl.PARAM1).getValue());
        taskUtil.fixTask(t);
        assertEquals(2, t.getParameters().size());
        assertEquals("${param1}", t.getParameters().get(DummyTaskTypeImpl.PARAM1).getValue());
        assertEquals("${param2}", t.getParameters().get(DummyTaskTypeImpl.PARAM2).getValue());
    }

    @Test
    public void testCopyTask() {
        Task t = taskUtil.copyTask(task1, "copiedTask");
        assertEquals("copiedTask", t.getName());
        assertEquals(task1.getType(), t.getType());
        assertEquals(task1.getParameters().size(), t.getParameters().size());
        for (Parameter par : task1.getParameters().values()) {
            assertEquals(par.getValue(), t.getParameters().get(par.getName()).getValue());
        }
    }

    @Test
    public void testValidate() {
        util.setConfigurationAttribute(config, ATT_FAIL, "true");

        assertTrue(taskUtil.validate(config).isEmpty());

        util.setConfigurationAttribute(config, ATT_DELAY, "boo");
        util.setConfigurationAttribute(config, ATT_DUMMY, null);
        util.setTaskParameter(task1, "foo", "bar");

        List<ValidationError> errors = taskUtil.validate(config);
        assertFalse(errors.isEmpty());
        assertEquals(3, errors.size());
        assertEquals(ValidationErrorType.INVALID_VALUE, errors.get(0).getType());
        assertEquals(ValidationErrorType.INVALID_PARAM, errors.get(1).getType());
        assertEquals(ValidationErrorType.MISSING, errors.get(2).getType());
    }

    @Test
    public void testCanCleanup() {
        assertFalse(taskUtil.canCleanup(task1));
        assertTrue(taskUtil.canCleanup(config));
        assertTrue(taskUtil.canCleanup(task2));
        config.getTasks().remove("task2");
        assertFalse(taskUtil.canCleanup(config));

        // (cleanup itself is suffiently tested in other tests
    }

    @Test
    public void testOrderTasksForCleanup() {
        Task task3 = fac.createTask();
        task3.setName("task3");
        task3.setType(TestTaskTypeImpl.NAME);
        util.addTaskToConfiguration(config, task3);
        Task task4 = fac.createTask();
        task4.setName("task4");
        task4.setType(TestTaskTypeImpl.NAME);
        util.addTaskToConfiguration(config, task4);
        Batch init = fac.createBatch();
        init.setName("@Initialize");
        util.addBatchToConfiguration(config, init);
        util.addBatchElement(init, task3);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");
        task3 = config.getTasks().get("task3");
        task4 = config.getTasks().get("task4");

        Batch batch1 = fac.createBatch();
        batch1.setName("batch1");
        util.addBatchToConfiguration(config, batch1);
        util.addBatchElement(batch1, task1);
        util.addBatchElement(batch1, task4);
        Batch batch2 = fac.createBatch();
        batch2.setName("batch2");
        util.addBatchToConfiguration(config, batch2);
        util.addBatchElement(batch2, task2);
        util.addBatchElement(batch2, task1);

        config = dao.save(config);

        List<Task> orderedTasks = taskUtil.orderTasksForCleanup(config);
        assertEquals(4, orderedTasks.size());
        assertEquals("task4", orderedTasks.get(0).getName());
        assertEquals("task1", orderedTasks.get(1).getName());
        assertEquals("task2", orderedTasks.get(2).getName());
        assertEquals("task3", orderedTasks.get(3).getName());
    }

    @Test
    public void testGetDependantRawValues() {
        List<String> rawValues =
                taskUtil.getDependentRawValues(
                        DummyAction.NAME, config.getAttributes().get(ATT_DUMMY), config);
        assertEquals(1, rawValues.size());
        assertEquals("false", rawValues.get(0));
    }
}
