/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

public class TestAccessLimitsSerialization {

    FilterFactory2 ff;

    Filter filter;

    MultiPolygon g;

    @Before
    public void setUp() throws Exception {
        ff = CommonFactoryFinder.getFilterFactory2(null);
        filter = ff.equal(ff.property("attribute"), ff.literal(3), true);
        g = (MultiPolygon) new WKTReader().read("MULTIPOLYGON(((0 0, 0 10, 10 10, 10 0, 0 0)))");
    }

    @Test
    public void testAccessLimits() throws Exception {
        AccessLimits limits = new AccessLimits(CatalogMode.MIXED);

        testObjectSerialization(limits);
    }

    @Test
    public void testSerializeWorkspaceAccessLimits() throws Exception {
        WorkspaceAccessLimits limits =
                new WorkspaceAccessLimits(CatalogMode.HIDE, true, true, true);

        testObjectSerialization(limits);
    }

    @Test
    public void testSerializeDataAccessLimits() throws Exception {
        DataAccessLimits limits = new DataAccessLimits(CatalogMode.CHALLENGE, filter);

        testObjectSerialization(limits);
    }

    @Test
    public void testSerializeFilterIncludeAsObject() throws Exception {
        DataAccessLimits limits = new DataAccessLimits(CatalogMode.CHALLENGE, Filter.INCLUDE);

        testObjectSerialization(limits);
    }

    @Test
    public void testSerializeFilterExcludeAsObject() throws Exception {
        DataAccessLimits limits = new DataAccessLimits(CatalogMode.CHALLENGE, Filter.EXCLUDE);

        testObjectSerialization(limits);
    }

    @Test
    public void testCoverageAccessLimits() throws Exception {
        CoverageAccessLimits limits = new CoverageAccessLimits(CatalogMode.MIXED, filter, g, null);

        testObjectSerialization(limits);
    }

    @Test
    public void testVectorAccessLimits() throws Exception {
        List<PropertyName> properties = new ArrayList<>();
        properties.add(ff.property("test"));
        VectorAccessLimits limits =
                new VectorAccessLimits(CatalogMode.MIXED, properties, filter, properties, filter);

        testObjectSerialization(limits);
    }

    @Test
    public void testWMSAccessLimits() throws Exception {
        List<PropertyName> properties = new ArrayList<>();
        properties.add(ff.property("test"));
        WMSAccessLimits limits = new WMSAccessLimits(CatalogMode.MIXED, filter, g, true);

        testObjectSerialization(limits);
    }

    private void testObjectSerialization(Serializable object) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);

            try (ObjectInputStream ois =
                    new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
                Object clone = ois.readObject();
                Assert.assertNotSame(object, clone);
                Assert.assertEquals(object, clone);
            }
        }
    }
}
