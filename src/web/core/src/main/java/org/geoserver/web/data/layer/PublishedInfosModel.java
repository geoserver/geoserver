/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.web.GeoServerApplication;
import org.opengis.filter.Filter;

/**
 * Layers and LayerGroups, filtered by workspaceInfo prefix if available.
 *
 * <p>Model contents are sorted by prefix name order.
 */
public class PublishedInfosModel extends LoadableDetachableModel<List<PublishedInfo>> {
    /**
     * Model lists all contents, override to change filter.
     *
     * @return accepts all
     */
    protected Filter getFilter() {
        return Predicates.acceptAll();
    }

    @Override
    protected List<PublishedInfo> load() {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        List<PublishedInfo> layers = new ArrayList<>();

        try (CloseableIterator<PublishedInfo> iterator =
                catalog.list(PublishedInfo.class, getFilter())) {
            iterator.forEachRemaining(layers::add);
        }
        Collections.sort(layers, Comparator.comparing(PublishedInfo::prefixedName));

        return layers;
    }
}
