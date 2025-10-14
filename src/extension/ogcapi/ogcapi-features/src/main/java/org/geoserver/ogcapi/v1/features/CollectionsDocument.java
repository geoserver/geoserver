/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.platform.ServiceException;
import org.geotools.api.filter.Filter;
import org.geotools.util.logging.Logging;

/**
 * A class representing the OGC API for Features server "collections" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JacksonXmlRootElement(localName = "Collections", namespace = "http://www.opengis.net/wfs/3.0")
@JsonPropertyOrder({"links", "collections"})
public class CollectionsDocument extends AbstractDocument {
    static final Logger LOGGER = Logging.getLogger(CollectionsDocument.class);
    private final GeoServer geoServer;
    private final List<String> crs;
    private final List<Consumer<CollectionDocument>> collectionDecorators = new ArrayList<>();
    private final boolean skipInvalid;

    public CollectionsDocument(GeoServer geoServer, List<String> crsList) {
        this.geoServer = geoServer;
        this.crs = crsList;
        /* this.extensions = extensions; */

        // build the self links
        String path = "ogc/features/v1/collections/";
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
    @SuppressWarnings("PMD.CloseResource") // closed while iterating over it
    public Iterator<CollectionDocument> getCollections() {
        CloseableIterator<FeatureTypeInfo> featureTypes =
                geoServer.getCatalog().list(FeatureTypeInfo.class, Filter.INCLUDE);
        return new Iterator<>() {

            CollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }
                while (featureTypes.hasNext()) {
                    FeatureTypeInfo featureType = featureTypes.next();
                    try {
                        next = getCollectionDocument(featureType, featureTypes);
                        return true;
                    } catch (Exception e) {
                        if (skipInvalid) {
                            LOGGER.log(Level.WARNING, "Skipping feature type " + featureType);
                        } else {
                            featureTypes.close();
                            throw new ServiceException("Failed to iterate over the feature types in the catalog", e);
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
    }

    private CollectionDocument getCollectionDocument(
            FeatureTypeInfo featureType, CloseableIterator<FeatureTypeInfo> featureTypes) throws IOException {
        List<String> crs = FeatureService.getFeatureTypeCRS(featureType, Collections.singletonList("#/crs"));
        CollectionDocument collection = new CollectionDocument(geoServer, featureType, crs, this.crs);
        for (Consumer<CollectionDocument> collectionDecorator : collectionDecorators) {
            collectionDecorator.accept(collection);
        }
        return collection;
    }

    public List<String> getCrs() {
        return crs;
    }

    public void addCollectionDecorator(Consumer<CollectionDocument> decorator) {
        this.collectionDecorators.add(decorator);
    }
}
