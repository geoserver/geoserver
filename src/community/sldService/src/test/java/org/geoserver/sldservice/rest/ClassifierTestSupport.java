/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import static org.junit.Assert.assertEquals;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.styling.Rule;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;

public class ClassifierTestSupport extends SLDServiceBaseTest {

    private static final int DEFAULT_INTERVALS = 2;

    protected SimpleFeatureCollection pointCollection, lineCollection;

    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    protected SimpleFeatureType dataType;

    protected SimpleFeature[] testFeatures;

    private Rule[] checkRules(String resultXml, int classes) {
        Rule[] rules = checkSLD(resultXml);
        assertEquals(classes, rules.length);
        return rules;
    }

    @Override
    protected String getServiceUrl() {
        return "classify";
    }
}