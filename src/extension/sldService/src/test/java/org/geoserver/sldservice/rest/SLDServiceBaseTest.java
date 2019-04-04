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

import java.io.StringReader;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.catalog.CatalogRESTTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xml.styling.SLDParser;

public abstract class SLDServiceBaseTest extends CatalogRESTTestSupport {

    protected SLDParser sldParser = new SLDParser(CommonFactoryFinder.getStyleFactory());

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // do not setup other test data, not used, not needed
        getTestData().addDefaultRasterLayer(getTestData().WORLD, getCatalog());
    }

    protected Rule[] checkSLD(String resultXml) {
        sldParser.setInput(new StringReader(resultXml));
        StyledLayerDescriptor descriptor = sldParser.parseSLD();
        return checkSLD(descriptor);
    }

    protected Rule[] checkSLD(StyledLayerDescriptor descriptor) {
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
