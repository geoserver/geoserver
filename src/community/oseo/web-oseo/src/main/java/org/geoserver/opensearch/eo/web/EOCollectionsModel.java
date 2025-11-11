/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.web.GeoServerApplication;
import org.geotools.feature.visitor.UniqueVisitor;

/** Wicket model to load the list of EO collections from the OpenSearch store. */
public class EOCollectionsModel extends LoadableDetachableModel<List<String>> {

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> load() {
        try {
            OpenSearchAccessProvider provider =
                    GeoServerApplication.get().getBeanOfType(OpenSearchAccessProvider.class);
            OpenSearchAccess openSearchAccess = provider.getOpenSearchAccess();
            UniqueVisitor uniqueIdentifiers = new UniqueVisitor("identifier");
            openSearchAccess.getCollectionSource().getFeatures().accepts(uniqueIdentifiers, null);
            return new ArrayList<>(new TreeSet<>(uniqueIdentifiers.getUnique()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load EO collections", e);
        }
    }
}
