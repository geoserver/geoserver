/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.StoreType;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.UploadMethod;
import it.geosolutions.geoserver.rest.encoder.GSGenericStoreEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.platform.resource.Resources;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.external.FileReference;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskRunnable;
import org.springframework.stereotype.Component;

@Component
public class FileRemotePublicationTaskTypeImpl extends AbstractRemotePublicationTaskTypeImpl {

    public static final String NAME = "RemoteFilePublication";

    public static final String PARAM_FILE = "file";

    public static final String PARAM_FILE_SERVICE = "fileService";

    public static final String PARAM_AUTO_VERSIONED = "auto-versioned";

    @PostConstruct
    public void initParamInfo() {
        super.initParamInfo();
        ParameterInfo fileService =
                new ParameterInfo(PARAM_FILE_SERVICE, extTypes.fileService, false);
        ParameterInfo autoVersioned =
                new ParameterInfo(PARAM_AUTO_VERSIONED, ParameterType.BOOLEAN, false);
        paramInfo.put(PARAM_FILE_SERVICE, fileService);
        paramInfo.put(PARAM_AUTO_VERSIONED, autoVersioned);
        paramInfo.put(
                PARAM_FILE,
                new ParameterInfo(PARAM_FILE, extTypes.file(true, false), false)
                        .dependsOn(fileService)
                        .dependsOn(autoVersioned));
    }

    @Override
    protected boolean createStore(
            ExternalGS extGS,
            GeoServerRESTManager restManager,
            StoreInfo store,
            TaskContext ctx,
            String name)
            throws IOException, TaskException {
        final StoreType storeType =
                store instanceof CoverageStoreInfo
                        ? StoreType.COVERAGESTORES
                        : StoreType.DATASTORES;

        boolean upload = false;

        FileReference fileRef =
                (FileReference)
                        ctx.getBatchContext()
                                .get(
                                        ctx.getParameterValues().get(PARAM_FILE),
                                        new BatchContext.Dependency() {
                                            @Override
                                            public void revert() throws TaskException {
                                                FileReference fileRef =
                                                        (FileReference)
                                                                ctx.getBatchContext()
                                                                        .get(
                                                                                ctx.getParameterValues()
                                                                                        .get(
                                                                                                PARAM_FILE));
                                                URI uri =
                                                        fileRef.getService()
                                                                .getURI(fileRef.getLatestVersion());
                                                restManager
                                                        .getStoreManager()
                                                        .update(
                                                                store.getWorkspace().getName(),
                                                                new GSGenericStoreEncoder(
                                                                        storeType,
                                                                        store.getWorkspace()
                                                                                .getName(),
                                                                        store.getType(),
                                                                        store.getName(),
                                                                        uri.toString(),
                                                                        true));
                                            }
                                        });
        URI uri = fileRef == null ? null : fileRef.getService().getURI(fileRef.getLatestVersion());
        if (uri == null) {
            try {
                uri = new URI(getLocation(store));
                upload = uri.getScheme() == null || uri.getScheme().toLowerCase().equals("file");
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
        if (upload) {
            final File file = Resources.fromURL(uri.toString()).file();
            return restManager
                    .getPublisher()
                    .createStore(
                            store.getWorkspace().getName(),
                            storeType,
                            name,
                            UploadMethod.FILE,
                            store.getType().toLowerCase(),
                            Files.probeContentType(file.toPath()),
                            file.toURI(),
                            null);
        } else {
            return restManager
                    .getStoreManager()
                    .create(
                            store.getWorkspace().getName(),
                            new GSGenericStoreEncoder(
                                    storeType,
                                    store.getWorkspace().getName(),
                                    store.getType(),
                                    name,
                                    uri.toString(),
                                    true));
        }
    }

    private String getLocation(StoreInfo storeInfo) {
        if (storeInfo instanceof CoverageStoreInfo) {
            return ((CoverageStoreInfo) storeInfo).getURL();
        } else {
            // this will work for shapefiles, which I believe is the only purely file-based
            // (non-database) vector store
            return ((DataStoreInfo) storeInfo).getConnectionParameters().get("url").toString();
        }
    }

    @Override
    protected void postProcess(
            GSResourceEncoder re, TaskContext ctx, TaskRunnable<GSResourceEncoder> update)
            throws TaskException {
        FileReference fileRef =
                (FileReference)
                        ctx.getBatchContext()
                                .get(
                                        ctx.getParameterValues().get(PARAM_FILE),
                                        new BatchContext.Dependency() {
                                            @Override
                                            public void revert() throws TaskException {
                                                FileReference fileRef =
                                                        (FileReference)
                                                                ctx.getBatchContext()
                                                                        .get(
                                                                                ctx.getParameterValues()
                                                                                        .get(
                                                                                                PARAM_FILE));
                                                String nativeName =
                                                        FilenameUtils.getBaseName(
                                                                fileRef.getLatestVersion());
                                                GSCoverageEncoder re = new GSCoverageEncoder(false);
                                                re.setNativeName(nativeName);
                                                re.setNativeCoverageName(nativeName);
                                                update.run(re);
                                            }
                                        });
        if (fileRef != null) {
            String nativeName = FilenameUtils.getBaseName(fileRef.getLatestVersion());
            ((GSCoverageEncoder) re).setNativeName(nativeName);
            ((GSCoverageEncoder) re).setNativeCoverageName(nativeName);
        }
    }

    @Override
    protected boolean neverReuseStore() {
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
