/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class GeoServerTemplateLoaderTest extends GeoServerSystemTestSupport {

    @Test
    public void test() throws Exception {
        File data = getTestData().getDataDirectoryRoot();

        File templates = new File(data, "templates");
        templates.mkdir();

        File featureTypes = new File(data, "featureTypes");
        featureTypes.mkdir();

        File featureType1 = new File(featureTypes, "ft1");
        featureType1.mkdir();

        File featureType2 = new File(featureTypes, "ft2");
        featureType2.mkdir();

        GeoServerResourceLoader resources = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        GeoServerTemplateLoader templateLoader = new GeoServerTemplateLoader(getClass(), resources);

        // test a path relative to templates
        File expected = new File(templates, "1.ftl");
        expected.createNewFile();

        File actual = (File) templateLoader.findTemplateSource("1.ftl");
        assertEquals(expected.getCanonicalPath(), actual.getCanonicalPath());

        // test a path relative to featureTypes
        expected = new File(featureType1, "2.ftl");
        expected.createNewFile();

        actual = (File) templateLoader.findTemplateSource("ft1/2.ftl");
        assertEquals(expected.getCanonicalPath(), actual.getCanonicalPath());

        actual = (File) templateLoader.findTemplateSource("2.ftl");
        assertNull(actual);

        // test loading relative to class
        Object source = templateLoader.findTemplateSource("FeatureSimple.ftl");
        assertNotNull(source);
        assertFalse(source instanceof File);
        templateLoader.getReader(source, "UTF-8");
    }

    @Test
    public void testRemoteType() throws Exception {
        GeoServerResourceLoader resources = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        GeoServerTemplateLoader loader = new GeoServerTemplateLoader(getClass(), resources);
        loader.findTemplateSource("header.ftl");
    }
}
