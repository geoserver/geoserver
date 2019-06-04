package org.geoserver.catalog.impl;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
import org.opengis.filter.FilterFactory;

public class CatalogPropertyAccessorTest {

    FilterFactory fac = CommonFactoryFinder.getFilterFactory();

    @Test
    public void testIndexedProperties() {
        FeatureTypeInfo fti = new FeatureTypeInfoImpl(null);
        fti.getMetadata().put("list", Lists.newArrayList("a", "b", "c"));
        fti.setSRS("EPSG:31370");

        // support index on map element
        assertEquals("a", fac.property("metadata.list[1]").evaluate(fti));
        assertEquals("b", fac.property("metadata.list[2]").evaluate(fti));
        assertEquals("c", fac.property("metadata.list[3]").evaluate(fti));

        // support indexed getter as well
        assertEquals(
                "m", fac.property("CRS.coordinateSystem.axis[1].unit").evaluate(fti).toString());
    }
}
