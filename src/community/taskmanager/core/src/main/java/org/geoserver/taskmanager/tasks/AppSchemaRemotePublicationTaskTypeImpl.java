/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.util.PlaceHolderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppSchemaRemotePublicationTaskTypeImpl extends FileRemotePublicationTaskTypeImpl {

    public static final String NAME = "RemoteAppSchemaPublication";

    public static final String PARAM_DB = "database";

    @Autowired private GeoServerDataDirectory dataDirectory;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    @PostConstruct
    public void initParamInfo() {
        super.initParamInfo();
        paramInfo.put(PARAM_DB, new ParameterInfo(PARAM_DB, extTypes.dbName, true));
    }

    @Override
    protected List<Resource> process(Resource res, ExternalGS extGS, TaskContext ctx)
            throws TaskException {

        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB);

        try {
            if (res.name().toUpperCase().endsWith(".ZIP")) {
                return processZip(res, db.getParameters(extGS));
            } else {
                return Collections.singletonList(processSingle(res, db.getParameters(extGS)));
            }
        } catch (IOException e) {
            throw new TaskException(e);
        }
    }

    private Resource processSingle(Resource res, Map<String, Serializable> parameters)
            throws IOException {
        Resource newRes = dataDirectory.get("/tmp").get(res.name());

        try (InputStream is = res.in()) {
            String template = IOUtils.toString(is, "UTF-8");
            String pub = PlaceHolderUtil.replacePlaceHolders(template, parameters);

            try (OutputStream os = newRes.out()) {
                os.write(pub.getBytes());
            }
        }

        return newRes;
    }

    private List<Resource> processZip(Resource res, Map<String, Serializable> parameters)
            throws IOException {
        List<Resource> resources = new ArrayList<>();

        try (ZipInputStream is = new ZipInputStream(res.in())) {
            for (ZipEntry entry = is.getNextEntry(); entry != null; entry = is.getNextEntry()) {
                String template = IOUtils.toString(is, "UTF-8");
                String pub = PlaceHolderUtil.replacePlaceHolders(template, parameters);

                Resource newRes = dataDirectory.get("/tmp").get(entry.getName());

                try (OutputStream os = newRes.out()) {
                    os.write(pub.getBytes());
                }

                if (newRes.name().equals(FilenameUtils.removeExtension(res.name()) + ".xml")) {
                    // put main file first
                    resources.add(0, newRes);
                } else {
                    resources.add(newRes);
                }
            }
        }

        return resources;
    }
}
