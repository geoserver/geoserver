/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.platform.resource.Resources;
import org.geoserver.taskmanager.external.ExternalGS;
import org.springframework.stereotype.Component;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.StoreType;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.UploadMethod;

@Component
public class FileRemotePublicationTaskTypeImpl extends AbstractRemotePublicationTaskTypeImpl {

    public static final String NAME = "RemoteFilePublication";
    
    @Override             
    protected boolean createStore(ExternalGS extGS, GeoServerRESTManager restManager, 
            StoreInfo store, Map<String, Object> parameterValues) throws IOException {
        final StoreType storeType = store instanceof CoverageStoreInfo ? 
                StoreType.COVERAGESTORES : StoreType.DATASTORES;
        final File file = Resources.fromURL(getURL(store)).file();
        return restManager.getPublisher().createStore(store.getWorkspace().getName(), storeType, store.getName(), 
                UploadMethod.FILE, store.getType().toLowerCase(), Files.probeContentType(file.toPath()), 
                file.toURI(), null);
    }
    
    private String getURL(StoreInfo storeInfo) {
        if (storeInfo instanceof CoverageStoreInfo) {
            return ((CoverageStoreInfo) storeInfo).getURL();
        } else {
            //this will work for shapefiles, which I believe is the only purely file-based
            //(non-database) vector store
            return ((DataStoreInfo) storeInfo).getConnectionParameters().get("url").toString();
        }
    }

    @Override
    protected boolean mustCleanUpStore() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }


}
