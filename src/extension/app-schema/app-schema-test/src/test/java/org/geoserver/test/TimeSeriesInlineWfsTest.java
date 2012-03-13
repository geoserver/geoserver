/*
 * Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import junit.framework.Test;

/**
 * This is to test using isList to group multiple values as a concatenated single value without
 * feature chaining.
 * 
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */

public class TimeSeriesInlineWfsTest extends TimeSeriesWfsTest {
    /**
     * Read-only test so can use one-time setup.
     * 
     */
    public static Test suite() {
        return new OneTimeTestSetup(new TimeSeriesInlineWfsTest());
    }

    protected NamespaceTestData buildTestData() {
        // only the test data is different since the config is slightly different (not using feature
        // chaining)
        // but the test cases from TimeSeriesWfsTest are the same
        return new TimeSeriesInlineMockData();
    }
}
