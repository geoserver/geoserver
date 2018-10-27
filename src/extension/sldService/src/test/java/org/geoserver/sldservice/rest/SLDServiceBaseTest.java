/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xml.styling.SLDParser;
import org.junit.After;
import org.junit.Before;

public abstract class SLDServiceBaseTest extends CatalogRESTTestSupport {

    protected Map<String, Object> attributes = new HashMap<String, Object>();

    protected Object responseEntity;

    protected ResourcePool resourcePool;

    protected FeatureTypeInfoImpl testFeatureTypeInfo;

    protected SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();

    protected StyleBuilder styleBuilder = new StyleBuilder();

    protected SLDParser sldParser = new SLDParser(CommonFactoryFinder.getStyleFactory());

    protected static final String FEATURETYPE_LAYER = "featuretype_layer";

    protected static final String COVERAGE_LAYER = "coverage_layer";

    @Before
    public void loadData() throws Exception {
        getTestData().addWorkspace(getTestData().WCS_PREFIX, getTestData().WCS_URI, getCatalog());
        getTestData().addDefaultRasterLayer(getTestData().WORLD, getCatalog());
    }

    @After
    public void restoreLayers() throws IOException {
        revertLayer(SystemTestData.BASIC_POLYGONS);
        removeWorkspace(getTestData().WCS_PREFIX);
        removeLayer(getTestData().WCS_PREFIX, getTestData().WORLD.getLocalPart());
    }

    protected Rule[] checkSLD(String resultXml) {
        sldParser.setInput(new StringReader(resultXml));
        StyledLayerDescriptor descriptor = sldParser.parseSLD();
        assertNotNull(descriptor);
        assertNotNull(descriptor.getStyledLayers());
        if (descriptor.getStyledLayers().length > 0) {
            StyledLayer layer = descriptor.getStyledLayers()[0];
            assertTrue(layer instanceof NamedLayer);
            NamedLayer namedLayer = (NamedLayer) layer;
            assertNotNull(namedLayer.getStyles());
            assertEquals(1, namedLayer.getStyles().length);
            Style style = namedLayer.getStyles()[0];
            assertNotNull(style.featureTypeStyles().toArray(new FeatureTypeStyle[0]));
            assertEquals(1, style.featureTypeStyles().toArray(new FeatureTypeStyle[0]).length);
            FeatureTypeStyle featureTypeStyle =
                    style.featureTypeStyles().toArray(new FeatureTypeStyle[0])[0];
            assertNotNull(featureTypeStyle.rules().toArray(new Rule[0]));
            return featureTypeStyle.rules().toArray(new Rule[0]);
        } else {
            return null;
        }
    }

    protected abstract String getServiceUrl();
}
