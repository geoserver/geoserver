/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.beans;

import com.google.common.collect.Lists;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.springframework.stereotype.Component;

/**
 * A dummy task type used for testing.
 *
 * @author Niels Charlier
 */
@Component
public class DummyTaskTypeImpl implements TaskType {

    public static final String NAME = "Dummy";

    public static final String PARAM1 = "param1";

    public static final String PARAM2 = "param2";

    private static final Map<String, ParameterInfo> PARAM_INFO =
            new LinkedHashMap<String, ParameterInfo>();

    static {
        PARAM_INFO.put(
                PARAM1,
                new ParameterInfo(
                        PARAM1,
                        new ParameterType() {
                            @Override
                            public List<String> getDomain(List<String> dependsOnRawValues) {
                                return Lists.newArrayList("true", "foo", "bar");
                            }

                            @Override
                            public Object parse(String value, List<String> dependsOnRawValues) {
                                if (getDomain(dependsOnRawValues).contains(value)) {
                                    return value;
                                } else {
                                    return null;
                                }
                            }

                            public List<String> getActions() {
                                return Lists.newArrayList("actionDummy");
                            }
                        },
                        true));

        PARAM_INFO.put(
                PARAM2,
                new ParameterInfo(
                                PARAM2,
                                new ParameterType() {
                                    @Override
                                    public List<String> getDomain(List<String> dependsOnRawValues) {
                                        if ("true".equals(dependsOnRawValues.get(0))) {
                                            return Lists.newArrayList("crash", "test", "dummy");
                                        } else {
                                            return Lists.newArrayList("dummy");
                                        }
                                    }

                                    @Override
                                    public Object parse(
                                            String value, List<String> dependsOnRawValues) {
                                        if (getDomain(dependsOnRawValues).contains(value)) {
                                            return value;
                                        } else {
                                            return null;
                                        }
                                    }

                                    public List<String> getActions() {
                                        return Lists.newArrayList("actionDummy");
                                    }
                                },
                                true)
                        .dependsOn(PARAM_INFO.get(PARAM1)));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return PARAM_INFO;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        throw new TaskException("this is dummy task, don't run it.");
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {}

    @Override
    public boolean supportsCleanup() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
