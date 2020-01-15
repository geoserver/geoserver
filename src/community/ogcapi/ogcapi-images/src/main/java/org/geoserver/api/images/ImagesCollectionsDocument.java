/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.api.APIException;
import org.geoserver.api.AbstractDocument;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.http.HttpStatus;

/**
 * A class representing the image server "collections" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JsonPropertyOrder({"links", "collections"})
public class ImagesCollectionsDocument extends AbstractDocument {

    static final Logger LOGGER = Logging.getLogger(ImagesCollectionsDocument.class);

    private final GeoServer gs;

    public ImagesCollectionsDocument(GeoServer gs) {
        this.gs = gs;

        // build the self links
        addSelfLinks("ogc/images/collections/");
    }

    public Iterator<ImagesCollectionDocument> getCollections() {

        CloseableIterator<CoverageInfo> coverages =
                gs.getCatalog().list(CoverageInfo.class, Filter.INCLUDE);
        boolean skipInvalid =
                gs.getGlobal().getResourceErrorHandling()
                        == ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS;
        return new Iterator<ImagesCollectionDocument>() {

            ImagesCollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }

                while (coverages.hasNext()) {
                    CoverageInfo coverage = coverages.next();
                    try {
                        if (coverage.getGridCoverageReader(null, null)
                                instanceof StructuredGridCoverage2DReader) {
                            ImagesCollectionDocument collection =
                                    new ImagesCollectionDocument(coverage, true);
                            next = collection;
                            return true;
                        }
                    } catch (Exception e) {
                        if (skipInvalid) {
                            LOGGER.log(Level.WARNING, "Skipping coverage  " + coverage);
                        } else {
                            throw new APIException(
                                    "InternalError",
                                    "Failed to iterate over the coverages in the catalog",
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    e);
                        }
                    }
                }

                return next != null;
            }

            @Override
            public ImagesCollectionDocument next() {
                ImagesCollectionDocument result = next;
                this.next = null;
                return result;
            }
        };
    }
}
