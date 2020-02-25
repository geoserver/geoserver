/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Test;
import org.opengis.filter.Filter;

public class XCQLTest {

    @Test
    public void testToFilter() throws Exception {
        String filter = "IN('foo','bar')";
        try {
            CQL.toFilter(filter);
            fail("filter should have thrown exception");
        } catch (CQLException e) {
        }

        Filter f1 = ECQL.toFilter(filter);
        Filter f2 = XCQL.toFilter(filter);
        assertEquals(f1, f2);
    }

    @Test
    public void testToFilterFallback() throws Exception {
        String filter = "id = 2";

        try {
            ECQL.toFilter(filter);
            fail("filter should have thrown exception");
        } catch (CQLException e) {
        }

        Filter f1 = CQL.toFilter(filter);
        Filter f2 = XCQL.toFilter(filter);
        assertEquals(f1, f2);
    }
}
