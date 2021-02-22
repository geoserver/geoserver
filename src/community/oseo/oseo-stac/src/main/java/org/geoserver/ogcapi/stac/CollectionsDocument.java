/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.IOException;
import java.util.Iterator;
import org.geoserver.ogcapi.AbstractDocument;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.ServiceException;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * A class representing the WFS3 server "collections" in a way that Jackson can easily translate to
 * JSON/YAML (and can be used as a Freemarker template model)
 */
@JsonPropertyOrder({"links", "collections"})
public class CollectionsDocument extends AbstractDocument {

    FeatureSource<FeatureType, Feature> collections;

    public CollectionsDocument(OpenSearchAccess openSearchAccess) throws IOException {
        this.collections = openSearchAccess.getCollectionSource();

        // build the self links
        String path = "ogc/stac/collections/";
        addSelfLinks(path);
    }

    @SuppressWarnings("PMD.CloseResource") // wrapped and returned
    public Iterator<CollectionDocument> getCollections() throws IOException {
        FeatureIterator<Feature> cit = collections.getFeatures().features();
        return new Iterator<CollectionDocument>() {

            CollectionDocument next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }

                boolean hasNext = cit.hasNext();
                if (!hasNext) {
                    cit.close();
                    return false;
                } else {
                    try {
                        Feature fc = cit.next();
                        CollectionDocument collection = new CollectionDocument(fc);

                        next = collection;
                        return true;
                    } catch (Exception e) {
                        cit.close();
                        throw new ServiceException(
                                "Failed to iterate over the collections in the EO catalog", e);
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
}
