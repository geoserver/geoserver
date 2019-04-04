/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.namespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * Simple detachable model listing all the available namespaces
 *
 * @author Gabriel Roldan
 */
@SuppressWarnings("serial")
public class NamespacesModel extends LoadableDetachableModel {

    @Override
    protected Object load() {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        List<NamespaceInfo> namespaces = new ArrayList<NamespaceInfo>(catalog.getNamespaces());
        Collections.sort(
                namespaces,
                new Comparator<NamespaceInfo>() {
                    public int compare(NamespaceInfo o1, NamespaceInfo o2) {
                        return o1.getPrefix().compareTo(o2.getPrefix());
                    }
                });
        return namespaces;
    }
}
