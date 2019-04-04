/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.FileReference;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CopyFileTaskTypeImpl implements TaskType {

    public static final String NAME = "CopyFile";

    public static final String PARAM_SOURCE_SERVICE = "sourceService";

    public static final String PARAM_TARGET_SERVICE = "targetService";

    public static final String PARAM_SOURCE_PATH = "sourcePath";

    public static final String PARAM_TARGET_PATH = "targetPath";

    public static final String PARAM_AUTO_VERSIONED = "auto-versioned";

    protected final Map<String, ParameterInfo> paramInfo =
            new LinkedHashMap<String, ParameterInfo>();

    @Autowired protected ExtTypes extTypes;

    @Override
    public String getName() {
        return NAME;
    }

    @PostConstruct
    public void initParamInfo() {
        ParameterInfo sourceService =
                new ParameterInfo(PARAM_SOURCE_SERVICE, extTypes.fileService, true);
        paramInfo.put(PARAM_SOURCE_SERVICE, sourceService);
        paramInfo.put(
                PARAM_SOURCE_PATH,
                new ParameterInfo(PARAM_SOURCE_PATH, extTypes.file(false, true), true)
                        .dependsOn(sourceService));
        ParameterInfo targetService =
                new ParameterInfo(PARAM_TARGET_SERVICE, extTypes.fileService, true);
        paramInfo.put(PARAM_TARGET_SERVICE, targetService);
        ParameterInfo autoVersioned =
                new ParameterInfo(PARAM_AUTO_VERSIONED, ParameterType.BOOLEAN, false);
        paramInfo.put(
                PARAM_TARGET_PATH,
                new ParameterInfo(PARAM_TARGET_PATH, extTypes.file(false, false), true)
                        .dependsOn(targetService)
                        .dependsOn(autoVersioned));
        paramInfo.put(
                PARAM_AUTO_VERSIONED,
                new ParameterInfo(PARAM_AUTO_VERSIONED, ParameterType.BOOLEAN, false));
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {
        final FileReference source =
                (FileReference)
                        ctx.getBatchContext().get(ctx.getParameterValues().get(PARAM_SOURCE_PATH));
        final FileReference target =
                (FileReference) ctx.getParameterValues().get(PARAM_TARGET_PATH);

        try {
            if (target.getLatestVersion().equals(target.getNextVersion())) {
                target.getService().delete(target.getNextVersion());
            }
            try (InputStream is = source.getService().read(source.getLatestVersion())) {
                target.getService().create(target.getNextVersion(), is);
            }
        } catch (IOException e) {
            throw new TaskException(e);
        }

        return new TaskResult() {

            @Override
            public void commit() throws TaskException {
                try {
                    if (!target.getLatestVersion().equals(target.getNextVersion())) {
                        target.getService().delete(target.getLatestVersion());
                    }
                } catch (IOException e) {
                    throw new TaskException(e);
                }
            }

            @Override
            public void rollback() throws TaskException {
                try {
                    if (!target.getLatestVersion().equals(target.getNextVersion())) {
                        target.getService().delete(target.getNextVersion());
                    }
                } catch (IOException e) {
                    throw new TaskException(e);
                }
            }
        };
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        final FileReference target =
                (FileReference) ctx.getParameterValues().get(PARAM_TARGET_PATH);

        try {
            target.getService().delete(target.getLatestVersion());
        } catch (IOException e) {
            throw new TaskException(e);
        }
    }
}
