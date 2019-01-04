/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.util.Arrays;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.SchemaException;
import org.geotools.feature.visitor.CountVisitor;
import org.geotools.feature.visitor.MaxVisitor;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.PropertyName;
import org.opengis.util.ProgressListener;

public class SecuredSimpleFeatureCollectionTest {

    FeatureVisitor lastVisitor = null;
    private ListFeatureCollection collection;

    @Before
    public void setup() throws SchemaException {
        GeoServerExtensionsHelper.singleton(
                "secureDataFactory", new DefaultSecureDataFactory(), SecuredObjectFactory.class);
        this.lastVisitor = null;
        SimpleFeatureType originalSchema =
                DataUtilities.createType(
                        "BasicPolygons", "the_geom:MultiPolygon:srid=4326,ID:String,value:int");
        collection =
                new ListFeatureCollection(originalSchema) {
                    public void accepts(FeatureVisitor visitor, ProgressListener progress)
                            throws IOException {
                        lastVisitor = visitor;
                    };
                };
    }

    @Test
    public void testMaxVisitorDelegation() throws SchemaException, IOException {
        MaxVisitor visitor =
                new MaxVisitor(CommonFactoryFinder.getFilterFactory2().property("value"));
        WrapperPolicy policy =
                WrapperPolicy.hide(
                        new VectorAccessLimits(CatalogMode.HIDE, null, null, null, null));
        assertOptimalVisit(visitor, policy);
    }

    @Test
    public void testMaxOnHiddenField() throws SchemaException, IOException {
        MaxVisitor visitor =
                new MaxVisitor(CommonFactoryFinder.getFilterFactory2().property("value"));
        PropertyName geom = CommonFactoryFinder.getFilterFactory2().property("the_geom");
        WrapperPolicy policy =
                WrapperPolicy.hide(
                        new VectorAccessLimits(
                                CatalogMode.HIDE, Arrays.asList(geom), null, null, null));
        SecuredSimpleFeatureCollection secured =
                new SecuredSimpleFeatureCollection(collection, policy);
        secured.accepts(visitor, null);
        assertNull(lastVisitor);
    }

    @Test
    public void testCountVisitorDelegation() throws SchemaException, IOException {
        FeatureVisitor visitor = new CountVisitor();
        WrapperPolicy policy =
                WrapperPolicy.hide(
                        new VectorAccessLimits(CatalogMode.HIDE, null, null, null, null));
        assertOptimalVisit(visitor, policy);
    }

    private void assertOptimalVisit(FeatureVisitor visitor, WrapperPolicy policy)
            throws IOException {
        SecuredSimpleFeatureCollection secured =
                new SecuredSimpleFeatureCollection(collection, policy);
        secured.accepts(visitor, null);
        assertSame(lastVisitor, visitor);
    }
}
