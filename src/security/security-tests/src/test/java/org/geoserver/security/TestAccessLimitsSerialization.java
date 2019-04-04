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
import junit.framework.TestCase;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

public class TestAccessLimitsSerialization extends TestCase {

    FilterFactory2 ff;

    Filter filter;

    MultiPolygon g;

    @Override
    protected void setUp() throws Exception {
        ff = CommonFactoryFinder.getFilterFactory2(null);
        filter = ff.equal(ff.property("attribute"), ff.literal(3), true);
        g = (MultiPolygon) new WKTReader().read("MULTIPOLYGON(((0 0, 0 10, 10 10, 10 0, 0 0)))");
    }

    public void testAccessLimits() throws Exception {
        AccessLimits limits = new AccessLimits(CatalogMode.MIXED);

        testObjectSerialization(limits);
    }

    public void testSerializeWorkspaceAccessLimits() throws Exception {
        WorkspaceAccessLimits limits =
                new WorkspaceAccessLimits(CatalogMode.HIDE, true, true, true);

        testObjectSerialization(limits);
    }

    public void testSerializeDataAccessLimits() throws Exception {
        DataAccessLimits limits = new DataAccessLimits(CatalogMode.CHALLENGE, filter);

        testObjectSerialization(limits);
    }

    public void testCoverageAccessLimits() throws Exception {
        CoverageAccessLimits limits = new CoverageAccessLimits(CatalogMode.MIXED, filter, g, null);

        testObjectSerialization(limits);
    }

    public void testVectorAccessLimits() throws Exception {
        List<PropertyName> properties = new ArrayList<PropertyName>();
        properties.add(ff.property("test"));
        VectorAccessLimits limits =
                new VectorAccessLimits(CatalogMode.MIXED, properties, filter, properties, filter);

        testObjectSerialization(limits);
    }

    public void testWMSAccessLimits() throws Exception {
        List<PropertyName> properties = new ArrayList<PropertyName>();
        properties.add(ff.property("test"));
        WMSAccessLimits limits = new WMSAccessLimits(CatalogMode.MIXED, filter, g, true);

        testObjectSerialization(limits);
    }

    private void testObjectSerialization(Serializable object) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(object);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        Object clone = ois.readObject();

        assertNotSame(object, clone);
        assertEquals(object, clone);
    }
}
