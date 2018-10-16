/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.data.Run.Status;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test data methods.
 *
 * @author Niels Charlier
 */
public class TaskManagerDataTest extends AbstractTaskManagerTest {

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private TaskManagerDataUtil util;

    private Configuration config;

    private Batch batch;

    @Before
    public void setupBatch() {
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task task = fac.createTask();
        task.setName("task");
        task.setType("testTask");
        util.addTaskToConfiguration(config, task);

        config = dao.save(config);

        batch = fac.createBatch();
        batch.setEnabled(true);

        batch.setName("my_batch");
        batch = dao.save(batch);
    }

    @After
    public void clearDataFromDatabase() {
        dao.delete(batch);
        dao.delete(config);
    }

    @Test
    public void testBatchElement() {
        assertEquals(
                Collections.singletonList(config.getTasks().get("task")),
                dao.getTasksAvailableForBatch(batch));

        Task task = config.getTasks().get("task");
        BatchElement el = util.addBatchElement(batch, task);
        batch = dao.save(batch);
        assertEquals(1, batch.getElements().size());
        assertEquals(Collections.emptyList(), dao.getTasksAvailableForBatch(batch));

        el = batch.getElements().get(0);

        // soft delete
        dao.remove(el);
        batch = dao.init(batch);
        assertTrue(batch.getElements().isEmpty());
        assertEquals(
                Collections.singletonList(config.getTasks().get("task")),
                dao.getTasksAvailableForBatch(batch));

        BatchElement el2 = util.addBatchElement(batch, task);
        assertEquals(el.getId(), el2.getId());
        batch = dao.save(batch);
        assertEquals(1, batch.getElements().size());
        assertEquals(Collections.emptyList(), dao.getTasksAvailableForBatch(batch));

        // hard delete
        dao.delete(batch.getElements().get(0));
        batch = dao.init(batch);
        assertTrue(batch.getElements().isEmpty());
        el2 = util.addBatchElement(batch, task);
        assertFalse(el.getId().equals(el2.getId()));
    }

    @Test
    public void testCloneConfiguration() {
        Task task = config.getTasks().get("task");
        assertEquals(0, task.getBatchElements().size());
        BatchElement el = util.addBatchElement(batch, task);
        batch = dao.save(batch);
        el = batch.getElements().get(0);
        config = dao.init(config);
        task = config.getTasks().get("task");
        assertEquals(1, task.getBatchElements().size());
        assertEquals(dao.reload(el), task.getBatchElements().get(0));

        Configuration config2 = dao.copyConfiguration("my_config");
        config2.setName("my_config2");
        config2 = dao.save(config2);
        task = config2.getTasks().get("task");
        assertEquals(0, task.getBatchElements().size());

        dao.delete(config2);

        batch.setConfiguration(config);
        batch = dao.save(batch);

        config2 = dao.copyConfiguration("my_config");
        config2.setName("my_config2");
        config2 = dao.save(config2);
        task = config2.getTasks().get("task");
        assertEquals(1, task.getBatchElements().size());
        assertFalse(config2.getBatches().get("my_batch").isEnabled());

        dao.delete(config2);
    }

    @Test
    public void testBatchRun() {
        assertTrue(dao.getCurrentBatchRuns(batch).isEmpty());

        BatchRun br = fac.createBatchRun();
        br.setBatch(batch);

        Run run = fac.createRun();
        run.setBatchRun(br);
        run.setStart(new Date(1000));
        run.setEnd(new Date(2000));
        run.setStatus(Status.READY_TO_COMMIT);
        br.getRuns().add(run);

        run = fac.createRun();
        run.setBatchRun(br);
        run.setStart(new Date(2000));
        run.setEnd(new Date(3000));
        run.setStatus(Status.COMMITTING);
        run.setMessage("foo");
        br.getRuns().add(run);

        run = fac.createRun();
        run.setBatchRun(br);
        run.setStart(new Date(3000));
        run.setEnd(new Date(4000));
        run.setStatus(Status.COMMITTED);
        br.getRuns().add(run);

        assertEquals(Status.COMMITTING, br.getStatus());

        br = dao.save(br);
        assertEquals(1, dao.getCurrentBatchRuns(batch).size());

        br = util.closeBatchRun(br, "foo");
        assertEquals(0, dao.getCurrentBatchRuns(batch).size());

        assertEquals(new Date(1000), br.getStart());
        assertEquals(new Date(4000), br.getEnd());
        assertEquals("foo", br.getMessage());
        assertEquals(Status.NOT_COMMITTED, br.getStatus());
    }
}
