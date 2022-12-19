/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.StoreType;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.UploadMethod;
import it.geosolutions.geoserver.rest.decoder.RESTDataStore;
import it.geosolutions.geoserver.rest.encoder.GSGenericStoreEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.external.FileReference;
import org.geoserver.taskmanager.schedule.BatchContext;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.ParameterType;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.tasks.AbstractRemotePublicationTaskTypeImpl.Finalizer;
import org.springframework.stereotype.Component;

@Component
public class FileRemotePublicationTaskTypeImpl extends AbstractRemotePublicationTaskTypeImpl {

    public static final String NAME = "RemoteFilePublication";

    public static final String PARAM_FILE = "file";

    public static final String PARAM_FILE_SERVICE = "fileService";

    public static final String PARAM_AUTO_VERSIONED = "auto-versioned";

    private static final String REMOTE_DIR = "uploaded-stores/store_";
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("yyMMddhhmmssMs");

    @Override
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

    private static String newLocationKey(StoreInfo store) {
        return REMOTE_DIR
                + store.getWorkspace().getName()
                + "_"
                + store.getName()
                + "_"
                + TIME_FMT.format(new Date())
                + "/";
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

                                                if (!isUpload(uri)) {
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
                                                                            store
                                                                                    .getConnectionParameters(),
                                                                            uri.toString(),
                                                                            true));
                                                }
                                            }
                                        });
        URI uri = fileRef == null ? null : fileRef.getService().getURI(fileRef.getLatestVersion());
        if (uri == null) {
            try {
                uri = new URI(getLocation(store));
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
        boolean upload = isUpload(uri);
        List<Resource> processedResources = null;
        String locationKey = null;
        if (upload) {
            if (!isSimpleUpload() || store.getType() == null) {
                locationKey = newLocationKey(store);
            }
            Resource resource;
            if (uri.getScheme().toLowerCase().equals("resource")) {
                resource = Resources.fromURL(uri.toString());
            } else {
                resource = Resources.fromPath(uri.toURL().getPath());
            }
            processedResources = process(resource, locationKey, extGS, restManager, ctx);
            if (locationKey == null && processedResources.size() > 1) {
                // can't simple upload anyway
                locationKey = newLocationKey(store);
            }
        }
        if (upload && locationKey == null) { // simple upload
            return restManager
                    .getPublisher()
                    .createStore(
                            store.getWorkspace().getName(),
                            storeType,
                            name,
                            UploadMethod.FILE,
                            store.getType().toLowerCase(),
                            Files.probeContentType(processedResources.get(0).file().toPath()),
                            processedResources.get(0).file().toURI(),
                            null);
        } else {
            String targetUri;
            if (upload) {
                for (Resource processedResource : processedResources) {
                    upload(restManager, locationKey, processedResource);
                }
                targetUri = "file:" + locationKey + "/" + processedResources.get(0).name();
            } else {
                targetUri = uri.toString();
            }
            return restManager
                    .getStoreManager()
                    .create(
                            store.getWorkspace().getName(),
                            new GSGenericStoreEncoder(
                                    storeType,
                                    store.getWorkspace().getName(),
                                    store.getType(),
                                    name,
                                    store.getConnectionParameters(),
                                    targetUri,
                                    true));
        }
    }

    private String getLocation(StoreInfo storeInfo) {
        if (storeInfo instanceof CoverageStoreInfo) {
            return ((CoverageStoreInfo) storeInfo).getURL();
        } else {
            // this will work for shapefiles and app-schemas
            // which I believe are the only file-based vector stores
            return ((DataStoreInfo) storeInfo).getConnectionParameters().get("url").toString();
        }
    }

    @Override
    protected void postProcess(
            StoreType storeType,
            ResourceInfo resource,
            GSResourceEncoder re,
            TaskContext ctx,
            Finalizer finalizer)
            throws TaskException {
        if (storeType == StoreType.COVERAGESTORES) {
            FileReference originalFileRef =
                    (FileReference) ctx.getParameterValues().get(PARAM_FILE);
            if (originalFileRef != null) {
                FileReference fileRef =
                        (FileReference)
                                ctx.getBatchContext()
                                        .get(
                                                originalFileRef,
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
                                                        if (finalizer.getEncoder()
                                                                instanceof GSCoverageEncoder) {
                                                            ((GSCoverageEncoder)
                                                                            finalizer.getEncoder())
                                                                    .setNativeCoverageName(
                                                                            nativeName);
                                                        }
                                                        finalizer
                                                                .getEncoder()
                                                                .setNativeName(nativeName);
                                                        finalizer.run();
                                                    }
                                                });
                finalizer.setFinalizeAtCommit(fileRef.equals(originalFileRef));

                String nativeName = FilenameUtils.getBaseName(fileRef.getLatestVersion());
                re.setNativeName(nativeName);
                if (re instanceof GSCoverageEncoder) {
                    ((GSCoverageEncoder) re).setNativeCoverageName(nativeName);
                }
            }
        }
    }

    @Override
    protected boolean cleanStore(
            GeoServerRESTManager restManager,
            StoreInfo store,
            StoreType storeType,
            String storeName,
            TaskContext ctx,
            boolean recurse)
            throws TaskException {
        FileReference fileRef = (FileReference) ctx.getParameterValues().get(PARAM_FILE);
        URI uri = fileRef == null ? null : fileRef.getService().getURI(fileRef.getLatestVersion());
        if (uri == null) {
            try {
                uri = new URI(getLocation(store));
            } catch (URISyntaxException e) {
                throw new TaskException(e);
            }
        }

        if (isUpload(uri) && storeType == StoreType.DATASTORES) {
            RESTDataStore restStore =
                    restManager.getReader().getDatastore(store.getWorkspace().getName(), storeName);
            String path = restStore.getConnectionParameters().get("url").replaceAll("file:", "");
            // get parent dir
            path = FilenameUtils.getPath(path);
            if (path.startsWith(REMOTE_DIR)) {
                if (!super.cleanStore(restManager, store, storeType, storeName, ctx, recurse)) {
                    return false;
                }
                return restManager.getResourceManager().delete(path);
            }
        }

        return super.cleanStore(restManager, store, storeType, storeName, ctx, recurse);
    }

    private boolean isUpload(URI uri) {
        return uri.getScheme() == null
                || uri.getScheme().toLowerCase().equals("file")
                || uri.getScheme().toLowerCase().equals("resource");
    }

    protected boolean isSimpleUpload() {
        return true;
    }

    protected void upload(GeoServerRESTManager restManager, String locationKey, Resource resource)
            throws TaskException, IOException {
        String path = locationKey + resource.name();
        try (InputStream is = resource.in()) {
            if (!restManager.getResourceManager().upload(path, is)) {
                throw new TaskException("Failed to upload store file " + resource.name());
            }
        }
    }

    protected List<Resource> process(
            Resource res,
            String locationKey,
            ExternalGS extGS,
            GeoServerRESTManager restManager,
            TaskContext ctx)
            throws TaskException {
        // hook for subclasses
        return Collections.singletonList(res);
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
