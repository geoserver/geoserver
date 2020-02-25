/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import java.io.IOException;
import java.util.List;
import org.geoserver.platform.resource.Resource;

/** Handles all the artifacts for a given process */
public interface ProcessArtifactsStore {

    public enum ArtifactType {
        Request,
        Response,
        Output,
        Temporary
    }

    /**
     * Retrieves a Resource for a store WPS artifact (input, output, temp file). The resource will
     * be UNDEFINED if the artifact is missing, and will be created on demand at the first access
     */
    Resource getArtifact(String executionId, ArtifactType type, String name);

    /** Lists all known directories containing process artifacts */
    List<Resource> listExecutionResourcess();

    /** Immediately removes all artifacts associated to the given execution id */
    void clearArtifacts(String executionId) throws IOException;
}
