/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature.retype;

import java.util.HashSet;
import java.util.Set;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;

/**
 * Takes a filter that eventually contains a fid filter and builds a new filter that contains the
 * same fids but with a different prefix
 *
 * @author Andrea Aime
 */
class FidTransformerVisitor extends DuplicatingFilterVisitor {
    private FeatureTypeMap map;

    public FidTransformerVisitor(FeatureTypeMap map) {
        super(CommonFactoryFinder.getFilterFactory2(null));
        this.map = map;
    }

    @Override
    public Object visit(Id filter, Object extraData) {
        Set ids = filter.getIDs();
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Invalid fid filter provides, has no fids inside");
        }
        Set<FeatureId> fids = new HashSet<>();
        for (Object o : ids) {
            FeatureId id = new FeatureIdImpl((String) o);
            FeatureId retyped =
                    RetypingFeatureCollection.reTypeId(
                            id, map.getFeatureType(), map.getOriginalFeatureType());
            fids.add(retyped);
        }
        return ff.id(fids);
    }
}
