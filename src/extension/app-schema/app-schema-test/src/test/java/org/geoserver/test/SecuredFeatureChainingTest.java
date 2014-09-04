/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.PropertyName;
import org.w3c.dom.Document;

/**
 * WFS GetFeature to test secured feature with GeoServer.
 * 
 * @author Victor Tey (CSIRO Earth Science and Resource Engineering)
 */
public class SecuredFeatureChainingTest extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureChainingMockData createTestData() {
        return new FeatureChainingMockData();
    }
    
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        
        springContextLocations.add("classpath:/test-data/ResourceAccessManagerContext.xml");
    }
    
    /**
     * Enable the Spring Security auth filters
     */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList((javax.servlet.Filter) GeoServerExtensions
                .bean("filterChainProxy"));
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addUser("cite_readfilter", "cite", null, Arrays.asList("ROLE_DUMMY"));
        addUser("cite_readatts", "cite", null, Arrays.asList("ROLE_DUMMY"));
        
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

        // populate the access manager
        TestResourceAccessManager tam = (TestResourceAccessManager) applicationContext
                .getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();
        FeatureTypeInfo gu = catalog.getFeatureTypeByName("gsml:GeologicUnit");

        // limits for mr readfilter
        Filter f = ff.equal(ff.property("purpose"), ff.literal("instance"), false);
        tam.putLimits("cite_readfilter", gu, new VectorAccessLimits(CatalogMode.HIDE, null, f,
                null, null));

        List<PropertyName> readAtts = Arrays.asList(ff.property("composition"), ff
                .property("outcropCharacter"));
        tam.putLimits("cite_readatts", gu, new VectorAccessLimits(CatalogMode.HIDE, readAtts, f,
                null, null));

    }

    /**
     * Test that denormalized data reports the correct number of features
     */
    @Test
    public void testDenormalisedFeaturesCount() {
        setRequestAuth("cite_readatts", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit" +
            "&maxFeatures=3&resultType=hits");
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit&maxFeatures=3 response:\n"
                + prettyString(doc));
        assertXpathEvaluatesTo("3", "//wfs:FeatureCollection/@numberOfFeatures", doc);

    }

    /**
     * Test that denormalized data reports the right output
     */
    @Test
    public void testSecureFeatureContent() {
        setRequestAuth("cite_readatts", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit&maxFeatures=3");
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit&maxFeatures=3 response:\n"
                + prettyString(doc));
        assertXpathCount(3, "//gsml:GeologicUnit", doc);
        assertXpathCount(0, "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:exposureColor", doc);
        assertXpathCount(0, "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:exposureColor", doc);
        assertXpathCount(0, "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:exposureColor", doc);

        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:composition", doc);
        assertXpathCount(2, "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:composition", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:composition", doc);

    }

}
