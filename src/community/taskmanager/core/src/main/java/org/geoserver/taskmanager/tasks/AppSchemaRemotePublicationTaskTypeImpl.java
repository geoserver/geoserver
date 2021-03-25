/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.StoreType;
import it.geosolutions.geoserver.rest.encoder.GSGenericStoreEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.external.impl.DbTableImpl;
import org.geoserver.taskmanager.schedule.BatchContext.Dependency;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.util.PlaceHolderUtil;
import org.geoserver.taskmanager.util.SqlUtil;
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

    private PlaceHolderUtil.ObjectTransform getTableTransform(
            Resource res,
            String locationKey,
            String content,
            DbSource db,
            GeoServerRESTManager restManager,
            TaskContext ctx)
            throws TaskException {
        final LayerInfo layer = (LayerInfo) ctx.getParameterValues().get(PARAM_LAYER);
        final StoreInfo store = layer.getResource().getStore();
        final StoreType storeType =
                store instanceof CoverageStoreInfo
                        ? StoreType.COVERAGESTORES
                        : StoreType.DATASTORES;
        return tableName ->
                SqlUtil.notQualified(
                        ((DbTableImpl)
                                        ctx.getBatchContext()
                                                .get(
                                                        new DbTableImpl(db, tableName),
                                                        new Dependency() {
                                                            @Override
                                                            public void revert()
                                                                    throws TaskException {
                                                                String newContent =
                                                                        PlaceHolderUtil
                                                                                .replaceObjectPlaceHolder(
                                                                                        content,
                                                                                        tableName ->
                                                                                                SqlUtil
                                                                                                        .notQualified(
                                                                                                                tableName));
                                                                try {
                                                                    try (OutputStream os =
                                                                            res.out()) {
                                                                        os.write(
                                                                                newContent
                                                                                        .getBytes());
                                                                    }
                                                                    upload(
                                                                            restManager,
                                                                            locationKey,
                                                                            res);

                                                                    restManager
                                                                            .getStoreManager()
                                                                            .update(
                                                                                    store.getWorkspace()
                                                                                            .getName(),
                                                                                    new GSGenericStoreEncoder(
                                                                                            storeType,
                                                                                            null,
                                                                                            null,
                                                                                            store
                                                                                                    .getName(),
                                                                                            null,
                                                                                            null,
                                                                                            null));
                                                                } catch (IOException e) {
                                                                    throw new TaskException(e);
                                                                }
                                                            }
                                                        }))
                                .getTableName());
    }

    @Override
    protected List<Resource> process(
            Resource res,
            String locationKey,
            ExternalGS extGS,
            GeoServerRESTManager restManager,
            TaskContext ctx)
            throws TaskException {

        try {
            if (res.name().toUpperCase().endsWith(".ZIP")) {
                return processZip(res, locationKey, extGS, restManager, ctx);
            } else {
                return Collections.singletonList(
                        processSingle(res, locationKey, extGS, restManager, ctx));
            }
        } catch (IOException e) {
            throw new TaskException(e);
        }
    }

    private Resource processSingle(
            Resource res,
            String locationKey,
            ExternalGS extGS,
            GeoServerRESTManager restManager,
            TaskContext ctx)
            throws IOException, TaskException {
        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB);
        Resource newRes = dataDirectory.get("/tmp").get(res.name());

        try (InputStream is = res.in()) {
            String template = IOUtils.toString(is, "UTF-8");
            String pub = PlaceHolderUtil.replacePlaceHolders(template, db.getParameters(extGS));
            pub =
                    PlaceHolderUtil.replaceObjectPlaceHolder(
                            pub, getTableTransform(newRes, locationKey, pub, db, restManager, ctx));

            try (OutputStream os = newRes.out()) {
                os.write(pub.getBytes());
            }
        }

        return newRes;
    }

    private List<Resource> processZip(
            Resource res,
            String locationKey,
            ExternalGS extGS,
            GeoServerRESTManager restManager,
            TaskContext ctx)
            throws IOException, TaskException {

        final DbSource db = (DbSource) ctx.getParameterValues().get(PARAM_DB);
        List<Resource> resources = new ArrayList<>();

        try (ZipInputStream is = new ZipInputStream(res.in())) {
            for (ZipEntry entry = is.getNextEntry(); entry != null; entry = is.getNextEntry()) {
                String template = IOUtils.toString(is, "UTF-8");
                String pub = PlaceHolderUtil.replacePlaceHolders(template, db.getParameters(extGS));

                Resource newRes = dataDirectory.get("/tmp").get(entry.getName());

                pub =
                        PlaceHolderUtil.replaceObjectPlaceHolder(
                                pub,
                                getTableTransform(newRes, locationKey, pub, db, restManager, ctx));
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

    @Override
    protected boolean isSimpleUpload() {
        return false;
    }
}
