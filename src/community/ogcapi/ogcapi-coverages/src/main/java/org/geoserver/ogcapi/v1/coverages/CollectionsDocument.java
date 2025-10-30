/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
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
    @JacksonXmlProperty(localName = "Links")
    public List<Link> getLinks() {
        return links;
    }

    @JacksonXmlProperty(localName = "Collection")
    public CloseableIterator<CollectionDocument> getCollections() {
        CloseableIterator<CoverageInfo> coverages = geoServer.getCatalog().list(CoverageInfo.class, Filter.INCLUDE);
        Iterator<CollectionDocument> collectionDocuments = new Iterator<>() {

            CollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }

                while (coverages.hasNext()) {
                    CoverageInfo coverage = coverages.next();
                    try {
                        CollectionDocument collection = getCollectionDocument(coverage, coverages);
                        next = collection;
                        return next != null;
                    } catch (Exception e) {
                        if (skipInvalid) {
                            LOGGER.log(Level.WARNING, "Skipping coverage type " + coverage.prefixedName());
                        } else {
                            LOGGER.log(Level.WARNING, "Failed to build collection for " + coverage.prefixedName(), e);
                            coverages.close();
                            throw new ServiceException("Failed to iterate over the coverage types in the catalog", e);
                        }
                    }
                }

                coverages.close();
                return false;
            }

            @Override
            public CollectionDocument next() {
                CollectionDocument result = next;
                this.next = null;
                return result;
            }
        };
        // Ensure CloseableIterator<CoverageInfo> is closed once consumed
        return new CloseableIteratorAdapter<>(collectionDocuments, coverages);
    }

    private CollectionDocument getCollectionDocument(CoverageInfo coverage, CloseableIterator<CoverageInfo> coverages)
            throws IOException {
        List<String> crs = CoveragesService.getCoverageCRS(coverage, Collections.singletonList("#/crs"));
        CollectionDocument collection = new CollectionDocument(geoServer, coverage, crs);
        return collection;
    }

    public List<String> getCrs() {
        return crs;
    }
}
