/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.util.Iterator;
import net.opengis.wfs.GetGmlObjectType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geotools.data.DataAccess;
import org.geotools.data.GmlObjectStore;
import org.geotools.util.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.GmlObjectId;

/**
 * Web Feature Service GetGmlObject operation.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class GetGmlObject {

    /** wfs config */
    WFSInfo wfs;

    /** the catalog */
    Catalog catalog;

    /** filter factory */
    FilterFactory filterFactory;

    public GetGmlObject(WFSInfo wfs, Catalog catalog) {
        this.wfs = wfs;
        this.catalog = catalog;
    }

    public void setFilterFactory(FilterFactory filterFactory) {
        this.filterFactory = filterFactory;
    }

    public Object run(GetGmlObjectType request) throws WFSException {

        // get the gml object id
        GmlObjectId id = (GmlObjectId) request.getGmlObjectId();

        // set up the hints
        Hints hints = new Hints();
        if (request.getTraverseXlinkDepth() != null) {
            Integer depth = GetFeature.traverseXlinkDepth(request.getTraverseXlinkDepth());
            hints.put(Hints.ASSOCIATION_TRAVERSAL_DEPTH, depth);
        }

        // walk through datastores finding one that is gmlobject aware
        for (Iterator d = catalog.getDataStores().iterator(); d.hasNext(); ) {
            DataStoreInfo dsInfo = (DataStoreInfo) d.next();
            DataAccess<? extends FeatureType, ? extends Feature> ds;
            try {
                ds = dsInfo.getDataStore(null);
            } catch (IOException e) {
                throw new WFSException(request, e);
            }

            if (ds instanceof GmlObjectStore) {
                try {
                    Object obj = ((GmlObjectStore) ds).getGmlObject(id, hints);
                    if (obj != null) {
                        return obj;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        throw new WFSException(request, "No such object: " + id);
    }
}
