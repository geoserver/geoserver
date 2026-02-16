/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import static org.geoserver.catalog.util.CloseableIteratorAdapter.filter;
import static org.geoserver.catalog.util.CloseableIteratorAdapter.transform;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.platform.ServiceException;
import org.geotools.api.filter.Filter;
import org.geotools.util.logging.Logging;

/**
 * A class representing the Coverages server "collections" in a way that Jackson can easily translate to JSON/YAML (and
 * can be used as a Freemarker template model)
 */
@JsonPropertyOrder({"links", "collections"})
public class CollectionsDocument extends AbstractDocument {

    static final Logger LOGGER = Logging.getLogger(CollectionsDocument.class);

    private final GeoServer geoServer;
    private final List<String> crs;
    private final boolean skipInvalid;

    public CollectionsDocument(GeoServer geoServer, List<String> crsList) {
        this.geoServer = geoServer;
        this.crs = crsList;

        // build the self links
        String path = "ogc/coverages/v1/collections/";
        addSelfLinks(path);
        skipInvalid =
                geoServer.getGlobal().getResourceErrorHandling() == ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS;
    }

    @Override
    public List<Link> getLinks() {
        return links;
    }

    /**
     * @implNote by returning a {@link CloseableIterator} we ensure it'll be closed by
     *     {@code org.geoserver.ogcapi.CloseableIteratorSerializer} for JSON output or
     *     {@code org.geoserver.ogcapi.AutoCloseableTracker} for HTML output
     */
    @SuppressWarnings("PMD.CloseResource")
    public CloseableIterator<CollectionDocument> getCollections() {
        Catalog catalog = geoServer.getCatalog();
        CloseableIterator<CoverageInfo> coverages = catalog.list(CoverageInfo.class, Filter.INCLUDE);
        CloseableIterator<CollectionDocument> collections =
                transform(coverages, cov -> getCollectionDocument(cov, coverages));
        return filter(collections, Objects::nonNull);
    }

    /**
     * @param coverage the CoverageInfo to create a collection document for
     * @param coverages to be closed early if an exception is caught and {@code skipInvalid == false}
     * @return the collection document for {@code coverage}, or {@code null} if an exception is caught and
     *     {@code skipInvalid == true}
     */
    private CollectionDocument getCollectionDocument(CoverageInfo coverage, CloseableIterator<CoverageInfo> coverages) {
        CollectionDocument collection = null;
        try {
            List<String> crs = CoveragesService.getCoverageCRS(coverage, Collections.singletonList("#/crs"));
            collection = new CollectionDocument(geoServer, coverage, crs);
        } catch (Exception e) {
            if (skipInvalid) {
                LOGGER.log(Level.WARNING, "Skipping coverage type " + coverage.prefixedName());
            } else {
                LOGGER.log(Level.WARNING, "Failed to build collection for " + coverage.prefixedName(), e);
                coverages.close();
                throw new ServiceException("Failed to iterate over the coverage types in the catalog", e);
            }
        }
        return collection;
    }

    public List<String> getCrs() {
        return crs;
    }
}
