/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import static org.geoserver.api.changeset.ChangesetTilesService.GET_RENDERED_COLLECTION_TILES;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geotools.util.logging.Logging;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class CheckpointCallback extends AbstractDispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(CheckpointCallback.class);
    public static final String X_CHECKPOINT = "x-checkpoint";

    private final ChangesetTilesService checkpointService;
    private final ChangesetIndexProvider index;

    public CheckpointCallback(
            ChangesetIndexProvider index, @Lazy ChangesetTilesService checkpointService) {
        this.index = index;
        this.checkpointService = checkpointService;
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        if (request.getServiceDescriptor().getService() instanceof ChangesetTilesService
                && GET_RENDERED_COLLECTION_TILES.equals(request.getRequest())) {
            String collectionId = (String) operation.getParameters()[0];
            try {
                CoverageInfo ci = checkpointService.getStructuredCoverageInfo(collectionId, false);
                String checkpoint = index.getLatestCheckpoint(ci);
                request.getHttpResponse().setHeader(X_CHECKPOINT, checkpoint);
            } catch (IOException e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Failed to compute checkpoint for collection: " + collectionId,
                        e);
            }
        }
        return super.responseDispatched(request, operation, result, response);
    }
}
