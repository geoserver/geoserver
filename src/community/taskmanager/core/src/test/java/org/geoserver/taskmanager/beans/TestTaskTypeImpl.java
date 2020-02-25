/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.beans;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Component;

/**
 * A task type used for testing.
 *
 * @author Niels Charlier
 */
@Component
public class TestTaskTypeImpl implements TaskType {

    public static final String NAME = "Test";

    public static final String PARAM_FAIL = "fail";

    public static final String PARAM_DELAY = "delay";

    public static final String PARAM_DELAY_COMMIT = "delay_commit";

    private static final Logger LOGGER = Logging.getLogger(TestTaskTypeImpl.class);

    private static final Map<String, ParameterInfo> PARAM_INFO =
            new LinkedHashMap<String, ParameterInfo>();

    static {
        PARAM_INFO.put(PARAM_FAIL, new ParameterInfo(PARAM_FAIL, ParameterType.BOOLEAN, false));
        PARAM_INFO.put(PARAM_DELAY, new ParameterInfo(PARAM_DELAY, ParameterType.INTEGER, false));
        PARAM_INFO.put(
                PARAM_DELAY_COMMIT,
                new ParameterInfo(PARAM_DELAY_COMMIT, ParameterType.INTEGER, false));
    }

    /** Contains the results of the runs that this task type has done. */
    protected Map<String, Integer> status = new HashMap<String, Integer>();

    public Map<String, Integer> getStatus() {
        return status;
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return PARAM_INFO;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        String identifier =
                ctx.getBatchContext().getBatchRun().getBatch().getFullName()
                        + ":"
                        + ctx.getTask().getFullName();
        LOGGER.log(Level.INFO, "running task " + identifier);

        status.put(identifier, 1);

        if (ctx.getParameterValues().containsKey(PARAM_DELAY)) {
            int delay = (Integer) ctx.getParameterValues().get(PARAM_DELAY);
            if (delay > 0) {
                LOGGER.log(Level.INFO, "waiting for " + delay + " milliseconds");
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    throw new TaskException(e);
                }
            }
        }

        status.put(identifier, 2);

        if (Boolean.TRUE.equals(ctx.getParameterValues().get(PARAM_FAIL))) {
            LOGGER.log(Level.INFO, "failing task " + identifier);
            throw new TaskException("purposely failed task");
        }

        return new TaskResult() {
            @Override
            public void commit() throws TaskException {
                LOGGER.log(Level.INFO, "committing task " + identifier);

                status.put(identifier, 3);

                if (ctx.getParameterValues().containsKey(PARAM_DELAY_COMMIT)) {
                    int delay = (Integer) ctx.getParameterValues().get(PARAM_DELAY_COMMIT);
                    if (delay > 0) {
                        LOGGER.log(Level.INFO, "waiting to commit for " + delay + " milliseconds");
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            throw new TaskException(e);
                        }
                    }
                }

                status.put(identifier, 4);
            }

            @Override
            public void rollback() {
                LOGGER.log(Level.INFO, "rolling back task " + identifier);
                status.put(identifier, 0);
            }
        };
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        throw new TaskException("unsupported");
    }

    @Override
    public boolean supportsCleanup() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
