/*
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import junit.framework.Test;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.wfs.WFSInfo;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.PropertyName;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * WFS GetFeature to test secured feature with GeoServer.
 * 
 * @author Victor Tey, CSIRO Information Management and Technology
 */
public class SecuredFeatureChainingTest extends AbstractAppSchemaWfsTestSupport {
    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new SecuredFeatureChainingTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        NamespaceTestData dataDirectory = new FeatureChainingMockData();
        try {
            populateDataDirectory(dataDirectory);
        } catch (IOException e) {
            // Test will fail if an exception is thrown on populateDataDirectory()
            LOGGER.warning("IOException while trying to populateDataDirectory:" + e.getMessage());
        }
        return dataDirectory;
    }

    protected String[] getSpringContextLocations() {
        String[] base = super.getSpringContextLocations();
        String[] extended = new String[base.length + 1];
        System.arraycopy(base, 0, extended, 0, base.length);
        extended[base.length] = "classpath:/test-data/ResourceAccessManagerContext.xml";
        return extended;
    }

    /**
     * Enable the Spring Security auth filters
     */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList((javax.servlet.Filter) GeoServerExtensions
                .bean("filterChainProxy"));
    }

    private void populateDataDirectory(NamespaceTestData dataDirectory) throws IOException {
        File security = new File(dataDirectory.getDataDirectoryRoot(), "security");
        security.mkdir();

        File users = new File(security, "users.properties");
        Properties props = new Properties();
        props.put("cite_readfilter", "cite,ROLE_DUMMY");
        props.put("cite_readatts", "cite,ROLE_DUMMY");
        props.store(new FileOutputStream(users), "");

    }

    protected void setUpInternal() throws Exception {
        super.setUpInternal();
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
    public void testDenormalisedFeaturesCount() {
        authenticate("cite_readatts", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&typename=gsml:GeologicUnit&maxFeatures=3&resultType=hits");
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit&maxFeatures=3 response:\n"
                + prettyString(doc));
        assertXpathEvaluatesTo("3", "//wfs:FeatureCollection/@numberOfFeatures", doc);

    }

    /**
     * Test that denormalized data reports the right output
     */
    public void testSecureFeatureContent() {
        authenticate("cite_readatts", "cite");
        Document doc = getAsDOM("wfs?request=GetFeature&typename=gsml:GeologicUnit&maxFeatures=3");
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
