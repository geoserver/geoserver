/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Arrays;
import java.util.List;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.decorators.SecuredObjects;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.collection.FilteringSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.visitor.CountVisitor;
import org.geotools.feature.visitor.MaxVisitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

public class FeatureCollectionDelegationTest extends GeoServerSystemTestSupport {
    private final WrapperPolicy policy =
            WrapperPolicy.readOnlyHide(new AccessLimits(CatalogMode.HIDE));
    private static final String FEATURE_TYPE_NAME = "testType";
    private FeatureVisitor lastVisitor = null;

    // These collections will delegate the MaxVisitor
    // As an example, ReTypingFeatureCollections may not delegate.
    private List<SimpleFeatureCollection> maxVisitorCollections;

    // These collection will delegate the CountVisitor
    private List<SimpleFeatureCollection> countVisitorCollections;

    @Before
    public void setUp() throws Exception {
        lastVisitor = null;

        GeometryFactory fac = new GeometryFactory();
        Point p = fac.createPoint(new Coordinate(8, 9));

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(FEATURE_TYPE_NAME);
        builder.add("geom", Point.class);

        SimpleFeatureType ft = builder.buildFeatureType();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(ft);
        b.add(p);

        ListFeatureCollection visitorCollection =
                new ListFeatureCollection(ft) {
                    public void accepts(FeatureVisitor visitor, ProgressListener progress) {
                        lastVisitor = visitor;
                    };

                    @Override
                    public SimpleFeatureCollection subCollection(Filter filter) {
                        if (filter == Filter.INCLUDE) {
                            return this;
                        } else {
                            return super.subCollection(filter);
                        }
                    }
                };
        SimpleFeatureSource featureSource = DataUtilities.source(visitorCollection);

        maxVisitorCollections =
                Arrays.asList(
                        new FeatureSizeFeatureCollection(
                                visitorCollection, featureSource, Query.ALL),
                        new FilteringSimpleFeatureCollection(visitorCollection, Filter.INCLUDE));
        countVisitorCollections =
                Arrays.asList(
                        new FeatureSizeFeatureCollection(
                                visitorCollection, featureSource, Query.ALL),
                        new FilteringSimpleFeatureCollection(visitorCollection, Filter.INCLUDE),
                        new RetypingFeatureCollection(
                                visitorCollection, visitorCollection.getSchema()),
                        (SimpleFeatureCollection) SecuredObjects.secure(visitorCollection, policy));
    }

    @Test
    public void testMaxVisitorDelegation() {
        MaxVisitor visitor =
                new MaxVisitor(CommonFactoryFinder.getFilterFactory2().property("value"));
        assertOptimalVisit(visitor, maxVisitorCollections);
    }

    @Test
    public void testCountVisitorDelegation() {
        FeatureVisitor visitor = new CountVisitor();
        assertOptimalVisit(visitor, countVisitorCollections);
    }

    private void assertOptimalVisit(
            FeatureVisitor visitor, List<SimpleFeatureCollection> collections) {
        collections.forEach(
                simpleFeatureCollection -> {
                    try {
                        lastVisitor = null;
                        simpleFeatureCollection.accepts(visitor, null);
                    } catch (Exception e) {
                        Assert.fail();
                    }
                    Assert.assertSame(lastVisitor, visitor);
                });
    }
}
