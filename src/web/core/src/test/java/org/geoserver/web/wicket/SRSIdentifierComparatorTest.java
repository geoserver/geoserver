/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.hamcrest.MatcherAssert.assertThat;

import org.geoserver.web.wicket.SRSProvider.SRS;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

public class SRSIdentifierComparatorTest {

    static final SRSProvider.SRSIdentifierComparator COMPARATOR =
            new SRSProvider.SRSIdentifierComparator();
    private static final Matcher<Integer> LOWER = Matchers.lessThan(0);

    private static final Matcher<Integer> EQUAL = Matchers.equalTo(0);
    private static final Matcher<Integer> HIGHER = Matchers.greaterThan(0);

    @Test
    public void testEPSGCommon() {
        // common numeric cases
        assertComparison("EPSG:4326", "EPSG:4326", EQUAL);
        assertComparison("EPSG:4326", "EPSG:4327", LOWER);
        assertComparison("EPSG:4327", "EPSG:4326", HIGHER);
    }

    private static void assertComparison(String id1, String id2, Matcher<Integer> equal) {
        assertThat(COMPARATOR.compare(new SRS(id1), new SRS(id2)), equal);
    }

    @Test
    public void testCrossAuthority() {
        assertComparison("EPSG:4326", "IAU:4326", LOWER);
        assertComparison("IAU:4326", "EPSG:4326", HIGHER);
    }

    @Test
    public void testNoAuthority() {
        assertComparison("EPSG:4326", "TEST", HIGHER);
        assertComparison("TEST", "EPSG:4326", LOWER);
    }
}
