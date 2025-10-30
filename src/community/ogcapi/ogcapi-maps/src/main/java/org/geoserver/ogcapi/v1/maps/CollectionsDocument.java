/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.PublishedInfo;
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

    @JacksonXmlProperty(localName = "Links")
    @Override
    public List<Link> getLinks() {
        return links;
    }

    @JacksonXmlProperty(localName = "Collection")
    public CloseableIterator<CollectionDocument> getCollections() {
        CloseableIterator<PublishedInfo> publisheds = geoServer.getCatalog().list(PublishedInfo.class, Filter.INCLUDE);
        Iterator<CollectionDocument> collectionDocuments = new Iterator<>() {

            CollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }
                while (publisheds.hasNext()) {
                    PublishedInfo published = publisheds.next();
                    try {
                        next = getCollectionDocument(published, publisheds);
                        return true;
                    } catch (Exception e) {
                        if (skipInvalid) {
                            LOGGER.log(Level.WARNING, "Skipping map type " + published.prefixedName());
                        } else {
                            publisheds.close();
                            throw new ServiceException("Failed to iterate over the map types in the catalog", e);
                        }
                    }
                }
                return next != null;
            }

            @Override
            public CollectionDocument next() {
                CollectionDocument result = next;
                this.next = null;
                return result;
            }
        };
        // Ensure CloseableIterator<FeatureTypeInfo> is closed once consumed
        return new CloseableIteratorAdapter<>(collectionDocuments, publisheds);
    }

    private CollectionDocument getCollectionDocument(
            PublishedInfo published, CloseableIterator<PublishedInfo> publisheds) throws IOException {
        CollectionDocument collection = new CollectionDocument(geoServer, published);
        for (Consumer<CollectionDocument> collectionDecorator : collectionDecorators) {
            collectionDecorator.accept(collection);
        }
        return collection;
    }

    public void addCollectionDecorator(Consumer<CollectionDocument> decorator) {
        this.collectionDecorators.add(decorator);
    }
}
