package com.boundlessgeo.gsr.translate.feature;

import com.boundlessgeo.gsr.translate.feature.ObjectIdRemappingFilterVisitor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsEqualTo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectIdRemappingFilterVisitorTest {

    @Test
    public void testRemap() throws CQLException {
        Filter filter = ECQL.toFilter("objectid=1");
        assertTrue(filter instanceof PropertyIsEqualTo);

        ObjectIdRemappingFilterVisitor visitor = new ObjectIdRemappingFilterVisitor("objectid", "");

        Object remappedFilter = filter.accept(visitor, null);
        assertTrue(remappedFilter instanceof Id);
    }

    @Test
    public void testSimplify() throws CQLException {
        Filter filter = ECQL.toFilter("objectid=objectid");
        assertTrue(filter instanceof PropertyIsEqualTo);

        ObjectIdRemappingFilterVisitor visitor = new ObjectIdRemappingFilterVisitor("objectid", "");

        Object remappedFilter = filter.accept(visitor, null);
        assertEquals(Filter.INCLUDE, remappedFilter);
    }
}
