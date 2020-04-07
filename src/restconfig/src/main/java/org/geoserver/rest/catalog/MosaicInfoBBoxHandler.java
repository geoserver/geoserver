/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;

/**
 * This class provides methods to update a mosaic native bounding box at the coverage info level
 * according to the bounds present in the index
 */
class MosaicInfoBBoxHandler {

    static final Logger LOGGER = Logging.getLogger(MosaicInfoBBoxHandler.class);

    private Catalog catalog;

    MosaicInfoBBoxHandler(Catalog catalog) {
        this.catalog = catalog;
    }

    void updateNativeBBox(
            String workspaceName, String storeName, StructuredGridCoverage2DReader reader)
            throws IOException {
        CoverageStoreInfo store = this.catalog.getCoverageStoreByName(workspaceName, storeName);
        updateNativeBBox(store, reader);
    }

    void updateNativeBBox(CoverageStoreInfo storeInfo, StructuredGridCoverage2DReader reader)
            throws IOException {
        List<CoverageInfo> coverages = this.catalog.getCoveragesByStore(storeInfo);
        try {
            if (reader == null) {
                reader =
                        (StructuredGridCoverage2DReader)
                                storeInfo.getGridCoverageReader(null, null);
            }
            for (CoverageInfo ci : coverages) {
                ReferencedEnvelope newBbox =
                        new ReferencedEnvelope(reader.getOriginalEnvelope(ci.getName()));
                ci.setNativeBoundingBox(newBbox);
                this.catalog.save(ci);
            }
        } catch (ClassCastException ex) {
            LOGGER.log(
                    Level.FINE,
                    "Store doesn't support harvesting."
                            + " Cannot update layer's native bounding box");
        }
    }
}
