/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wfs.GetGmlObjectType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geotools.api.data.DataAccess;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.identity.GmlObjectId;
import org.geotools.data.GmlObjectStore;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * Web Feature Service GetGmlObject operation.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class GetGmlObject {
    static final Logger LOGGER = Logging.getLogger(GetGmlObject.class);

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
        for (DataStoreInfo dsInfo : catalog.getDataStores()) {
            DataAccess<? extends FeatureType, ? extends Feature> ds;
            try {
                ds = dsInfo.getDataStore(null);
            } catch (IOException e) {
                throw new WFSException(request, e);
            }

            if (ds instanceof GmlObjectStore store) {
                try {
                    Object obj = store.getGmlObject(id, hints);
                    if (obj != null) {
                        return obj;
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "", e);
                }
            }
        }

        throw new WFSException(request, "No such object: " + id);
    }
}
