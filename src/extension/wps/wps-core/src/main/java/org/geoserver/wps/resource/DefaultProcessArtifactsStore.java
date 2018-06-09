/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;

/**
 * Default implementation that will store the artifacts on the file system inside
 * $GEOSEVER_DATA_DIR/temp/wps, or if otherwise configured, in the folder setup in
 * WPSINfo.outputStorageDirectory
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultProcessArtifactsStore implements ProcessArtifactsStore {

    ResourceStore store;

    public void setResourceStore(ResourceStore store) {
        this.store = store;
    }

    @Override
    public Resource getArtifact(String executionId, ArtifactType type, String name) {
        Resource result = null;
        if (type == ArtifactType.Request) {
            result = store.get(Paths.path(executionId, "request.dat"));
        } else if (type == ArtifactType.Response) {
            result = store.get(Paths.path(executionId, "response.xml"));
        } else if (type == ArtifactType.Output) {
            result = store.get(Paths.path(executionId, "out", name));
        } else {
            result = store.get(Paths.path(executionId, "tmp", name));
        }

        return result;
    }

    @Override
    public List<Resource> listExecutionResourcess() {
        List<Resource> result = new ArrayList<>();
        for (Resource r : store.get(Paths.BASE).list()) {
            if (r.getType() == Type.DIRECTORY) {
                result.add(r);
            }
        }

        return result;
    }

    @Override
    public void clearArtifacts(String executionId) throws IOException {
        Resource resource = store.get(executionId);
        resource.delete();
    }
}
