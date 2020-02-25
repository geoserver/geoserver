/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.geoserver.api.AbstractDocument;
import org.geoserver.api.Link;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.opengis.filter.Filter;

/**
 * A class representing the WFS3 server "collections" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JacksonXmlRootElement(localName = "Collections", namespace = "http://www.opengis.net/wfs/3.0")
@JsonPropertyOrder({"links", "collections"})
public class CollectionsDocument extends AbstractDocument {

    private final GeoServer geoServer;
    private final List<String> crs;
    private final List<Consumer<CollectionDocument>> collectionDecorators = new ArrayList<>();

    public CollectionsDocument(GeoServer geoServer, List<String> crsList) {
        this.geoServer = geoServer;
        this.crs = crsList;
        /* this.extensions = extensions; */

        // build the self links
        String path = "ogc/features/collections/";
        addSelfLinks(path);
    }

    @JacksonXmlProperty(localName = "Links")
    public List<Link> getLinks() {
        return links;
    }

    @JacksonXmlProperty(localName = "Collection")
    public Iterator<CollectionDocument> getCollections() {
        CloseableIterator<FeatureTypeInfo> featureTypes =
                geoServer.getCatalog().list(FeatureTypeInfo.class, Filter.INCLUDE);
        return new Iterator<CollectionDocument>() {

            CollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }

                boolean hasNext = featureTypes.hasNext();
                if (!hasNext) {
                    featureTypes.close();
                    return false;
                } else {
                    try {
                        FeatureTypeInfo featureType = featureTypes.next();
                        List<String> crs =
                                FeatureService.getFeatureTypeCRS(
                                        featureType, Collections.singletonList("#/crs"));
                        CollectionDocument collection =
                                new CollectionDocument(geoServer, featureType, crs);
                        for (Consumer<CollectionDocument> collectionDecorator :
                                collectionDecorators) {
                            collectionDecorator.accept(collection);
                        }

                        next = collection;
                        return true;
                    } catch (Exception e) {
                        featureTypes.close();
                        throw new ServiceException(
                                "Failed to iterate over the feature types in the catalog", e);
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

    public List<String> getCrs() {
        return crs;
    }

    public void addCollectionDecorator(Consumer<CollectionDocument> decorator) {
        this.collectionDecorators.add(decorator);
    }
}
