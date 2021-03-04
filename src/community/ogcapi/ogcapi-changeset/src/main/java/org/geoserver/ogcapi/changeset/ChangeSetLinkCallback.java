/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.changeset;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.DocumentCallback;
import org.geoserver.ogcapi.Link;
import org.geoserver.ogcapi.tiles.TilesDocument;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Component;

/** Adds the multitile changeset links to the tiles resource */
@Component
public class ChangeSetLinkCallback implements DocumentCallback {

    static final Logger LOGGER = Logging.getLogger(ChangeSetLinkCallback.class);

    ChangesetTilesService changesetService;

    public ChangeSetLinkCallback(ChangesetTilesService changesetService) {
        this.changesetService = changesetService;
    }

    @Override
    public void apply(Request dr, AbstractDocument document) {
        if (document instanceof TilesDocument) {
            TilesDocument tiles = (TilesDocument) document;
            try {
                CoverageInfo coverage =
                        changesetService.getStructuredCoverageInfo(tiles.getId(), false);
                if (coverage != null) {
                    List<Link> links =
                            APIRequestInfo.get()
                                    .getLinksFor(
                                            "ogc/tiles/collections/"
                                                    + tiles.getId()
                                                    + "/map/{styleId}/{tileMatrixSetId}",
                                            ChangeSet.class,
                                            "Changeset as ",
                                            null,
                                            null,
                                            "multitile",
                                            false);
                    tiles.getLinks().addAll(links);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "", e);
            }
        }
    }
}
