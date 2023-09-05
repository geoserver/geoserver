/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.platform.ServiceException;
import org.geotools.api.filter.Filter;

/**
 * A class representing the Maps service "collections" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JsonPropertyOrder({"links", "collections"})
public class CollectionsDocument extends AbstractDocument {

    private final GeoServer geoServer;
    private final List<Consumer<CollectionDocument>> collectionDecorators = new ArrayList<>();

    public CollectionsDocument(GeoServer geoServer) {
        this.geoServer = geoServer;

        // build the self links
        String path = "ogc/maps/v1/collections/";
        addSelfLinks(path);
    }

    @JacksonXmlProperty(localName = "Links")
    @Override
    public List<Link> getLinks() {
        return links;
    }

    @JacksonXmlProperty(localName = "Collection")
    public Iterator<CollectionDocument> getCollections() {
        @SuppressWarnings("PMD.CloseResource") // wrapped and returned
        CloseableIterator<PublishedInfo> publisheds =
                geoServer.getCatalog().list(PublishedInfo.class, Filter.INCLUDE);
        return new Iterator<CollectionDocument>() {

            CollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }

                boolean hasNext = publisheds.hasNext();
                if (!hasNext) {
                    publisheds.close();
                    return false;
                } else {
                    try {
                        PublishedInfo published = publisheds.next();
                        CollectionDocument collection =
                                new CollectionDocument(geoServer, published);
                        for (Consumer<CollectionDocument> collectionDecorator :
                                collectionDecorators) {
                            collectionDecorator.accept(collection);
                        }

                        next = collection;
                        return true;
                    } catch (Exception e) {
                        publisheds.close();
                        throw new ServiceException(
                                "Failed to iterate over the published info in the catalog", e);
                    }
                }
            }

            @Override
            public CollectionDocument next() {
                CollectionDocument result = next;
                this.next = null;
                return result;
            }
        };
    }

    public void addCollectionDecorator(Consumer<CollectionDocument> decorator) {
        this.collectionDecorators.add(decorator);
    }
}
