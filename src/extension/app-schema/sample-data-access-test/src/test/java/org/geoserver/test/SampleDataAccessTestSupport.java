/*
 * Copyright (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geoserver.data.test.TestData;
import org.geotools.data.SampleDataAccess;

/**
 * Abstract base class for test cases that test integration of {@link SampleDataAccess} with
 * GeoServer.
 * 
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public abstract class SampleDataAccessTestSupport extends GeoServerSystemTestSupport {

    @Override
    protected SampleDataAccessMockData createTestData() throws Exception {
        return new SampleDataAccessMockData();
    }

}
