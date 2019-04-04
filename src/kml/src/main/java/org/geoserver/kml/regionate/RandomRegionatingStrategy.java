/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.regionate;

import java.sql.Connection;
import org.geoserver.config.GeoServer;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.spatial.BBOX;

/**
 * This strategy just return the features as they come from the db and leave the pyramid structure
 * do the rest. Of course is the data is inserted in the db in a way that makes it return features
 * in some linear way the distribution won't look good. But the same might happen to attribute
 * sorting as well, for example, when the high values of the sorting attribute do concentrate in a
 * specific area instead of being evenly spread out.
 *
 * @author Andrea Aime
 */
public class RandomRegionatingStrategy extends CachedHierarchyRegionatingStrategy {

    public RandomRegionatingStrategy(GeoServer gs) {
        super(gs);
    }

    @Override
    public FeatureIterator getSortedFeatures(
            GeometryDescriptor geom,
            ReferencedEnvelope latLongEnv,
            ReferencedEnvelope nativeEnv,
            Connection cacheConn)
            throws Exception {
        FeatureSource fs = featureType.getFeatureSource(null, null);

        // build the bbox filter
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

        BBOX filter =
                ff.bbox(
                        geom.getLocalName(),
                        nativeEnv.getMinX(),
                        nativeEnv.getMinY(),
                        nativeEnv.getMaxX(),
                        nativeEnv.getMaxY(),
                        null);

        // build an optimized query (only the necessary attributes
        Query q = new Query();
        q.setFilter(filter);
        // TODO: enable this when JTS learns how to compute centroids
        // without triggering the
        // generation of Coordinate[] out of the sequences...
        // q.setHints(new Hints(Hints.JTS_COORDINATE_SEQUENCE_FACTORY,
        // PackedCoordinateSequenceFactory.class));
        q.setPropertyNames(new String[] {geom.getLocalName()});

        // return the reader
        return fs.getFeatures(q).features();
    }
}
