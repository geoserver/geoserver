/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.Iterator;
import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.ogcapi.Link;
import org.geoserver.platform.ServiceException;
import org.geotools.api.filter.Filter;

/**
 * A class representing the DGGS server "collections" in a way that Jackson can easily translate to JSON/YAML (and can
 * be used as a Freemarker template model)
 */
@JsonPropertyOrder({"links", "collections"})
public class CollectionsDocument extends AbstractDocument {

    private final GeoServer geoServer;

    public CollectionsDocument(GeoServer geoServer) {
        this.geoServer = geoServer;

        // build the self links
        String path = "ogc/dggs/v1/collections/";
        addSelfLinks(path);
    }

    @Override
    @JacksonXmlProperty(localName = "Links")
    public List<Link> getLinks() {
        return links;
    }

    @JacksonXmlProperty(localName = "Collection")
    @SuppressWarnings("PMD.EmptyControlStatement")
    public CloseableIterator<CollectionDocument> getCollections() {
        CloseableIterator<FeatureTypeInfo> featureTypes =
                geoServer.getCatalog().list(FeatureTypeInfo.class, Filter.INCLUDE);
        Iterator<CollectionDocument> collectionDocuments = new Iterator<>() {

            CollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }

                FeatureTypeInfo featureType = null;
                while (featureTypes.hasNext() && !DGGSService.isDGGSType((featureType = featureTypes.next())))
                    ; // nothing to do here, the iteration does it all

                if (featureType == null || !DGGSService.isDGGSType(featureType)) {
                    featureTypes.close();
                    return false;
                } else {
                    try {
                        CollectionDocument collection = new CollectionDocument(geoServer, featureType);

                        next = collection;
                        return true;
                    } catch (Exception e) {
                        featureTypes.close();
                        throw new ServiceException("Failed to iterate over the feature types in the catalog", e);
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
        // Ensure CloseableIterator<FeatureTypeInfo> is closed once consumed
        return new CloseableIteratorAdapter<>(collectionDocuments, featureTypes);
    }
}
