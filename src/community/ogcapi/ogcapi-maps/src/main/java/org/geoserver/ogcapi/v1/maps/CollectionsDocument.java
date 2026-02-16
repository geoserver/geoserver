/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.geoserver.catalog.util.CloseableIteratorAdapter.filter;
import static org.geoserver.catalog.util.CloseableIteratorAdapter.transform;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.platform.ServiceException;
import org.geotools.api.filter.Filter;
import org.geotools.util.logging.Logging;

/**
 * A class representing the Maps service "collections" in a way that Jackson can easily translate to JSON/YAML (and can
 * be used as a Freemarker template model)
 */
@JsonPropertyOrder({"links", "collections"})
public class CollectionsDocument extends AbstractDocument {
    static final Logger LOGGER = Logging.getLogger(CollectionsDocument.class);
    private final GeoServer geoServer;
    private final List<Consumer<CollectionDocument>> collectionDecorators = new ArrayList<>();
    private final boolean skipInvalid;

    public CollectionsDocument(GeoServer geoServer) {
        this.geoServer = geoServer;

        // build the self links
        String path = "ogc/maps/v1/collections/";
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
        CloseableIterator<PublishedInfo> publisheds = geoServer.getCatalog().list(PublishedInfo.class, Filter.INCLUDE);

        CloseableIterator<CollectionDocument> collections =
                transform(publisheds, published -> getCollectionDocument(published, publisheds));

        return filter(collections, Objects::nonNull);
    }

    /**
     * @param published the published info to create a collection document for
     * @param publisheds to be closed early if an exception is caught and {@code skipInvalid == false}
     * @return the collection document for {@code published}, or {@code null} if an exception is caught and
     *     {@code skipInvalid == true}
     */
    private CollectionDocument getCollectionDocument(
            PublishedInfo published, CloseableIterator<PublishedInfo> publisheds) {
        CollectionDocument collection = null;
        try {
            collection = new CollectionDocument(geoServer, published);
            for (Consumer<CollectionDocument> collectionDecorator : collectionDecorators) {
                collectionDecorator.accept(collection);
            }
        } catch (Exception e) {
            if (skipInvalid) {
                LOGGER.log(Level.WARNING, "Skipping map type " + published.prefixedName());
            } else {
                publisheds.close();
                throw new ServiceException("Failed to iterate over the map types in the catalog", e);
            }
        }
        return collection;
    }

    public void addCollectionDecorator(Consumer<CollectionDocument> decorator) {
        this.collectionDecorators.add(decorator);
    }
}
