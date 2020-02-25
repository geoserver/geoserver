/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.api.images.ImageListener;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.stereotype.Component;

/**
 * Implements the core of the changeset support functionality:
 *
 * <ul>
 *   <li>Connects to a {@link ChangesetIndexProvider}
 *   <li>Records each change from {@link org.geoserver.api.images.ImagesService} into it, creating
 *       new checkpoints
 *   <li>Allows to get a list of modified areas given a layer and a checkpoint
 * </ul>
 */
@Component
public class ChangesetImageListener implements ImageListener {

    static final Logger LOGGER = Logging.getLogger(ChangesetImageListener.class);

    private final GWC gwc;
    private final ChangesetIndexProvider indexProvider;
    private final Catalog catalog;

    public ChangesetImageListener(Catalog catalog, ChangesetIndexProvider indexProvider, GWC gwc)
            throws IOException {
        this.indexProvider = indexProvider;
        this.gwc = gwc;
        this.catalog = catalog;
    }

    @Override
    public void imageAdded(CoverageInfo ci, SimpleFeature feature) {
        try {
            indexProvider.addCheckpoint(ci, feature);
            truncateTilesForCoverage(ci, feature);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failure while trying to record a image added checkpoint", e);
        }
    }

    @Override
    public void imageRemoved(CoverageInfo ci, SimpleFeature feature) {
        try {
            indexProvider.addCheckpoint(ci, feature);
            truncateTilesForCoverage(ci, feature);
        } catch (IOException e) {
            LOGGER.log(
                    Level.SEVERE, "Failure while trying to record a image removed checkpoint", e);
        }
    }

    /** Truncates all the GWC tiles involved in this change */
    private void truncateTilesForCoverage(CoverageInfo ci, SimpleFeature feature) {
        catalog.getLayers(ci)
                .stream()
                .map(l -> gwc.getTileLayer(l))
                .filter(tl -> tl != null)
                .forEach(tl -> truncateTilesForTileLayer(tl, feature));
    }

    /** Truncates all the GWC tiles covered by the specified feature */
    private void truncateTilesForTileLayer(GeoServerTileLayer tl, SimpleFeature feature) {
        try {
            // TODO: this could be optimized, if a multipolygon only truncate the single
            // polygons
            gwc.truncate(tl.getName(), ReferencedEnvelope.reference(feature.getBounds()));
        } catch (GeoWebCacheException e) {
            LOGGER.log(Level.SEVERE, "Failed to truncate tiles for " + tl, e);
        }
    }
}
