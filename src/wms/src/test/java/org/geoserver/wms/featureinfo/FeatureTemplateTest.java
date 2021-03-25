/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.featureinfo.dummy.Dummy;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

public class FeatureTemplateTest extends WMSTestSupport {

    @Test
    public void testWithDateAndBoolean() throws Exception {

        SimpleFeatureSource source = getFeatureSource(MockData.PRIMITIVEGEOFEATURE);
        SimpleFeatureCollection fc = source.getFeatures();
        try (SimpleFeatureIterator i = fc.features()) {
            SimpleFeature f = i.next();

            FeatureTemplate template = new FeatureTemplate();
            try {
                template.description(f);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "", e);
                fail("template threw exception on null value. " + e.getMessage());
            }
        }
    }

    @Test
    public void testRawValue() throws Exception {
        SimpleFeatureSource source = getFeatureSource(MockData.PRIMITIVEGEOFEATURE);
        SimpleFeatureCollection fc = source.getFeatures();
        try (SimpleFeatureIterator i = fc.features()) {
            SimpleFeature f = i.next();

            FeatureTemplate template = new FeatureTemplate();
            try {
                template.template(f, "rawValues.ftl", FeatureTemplateTest.class);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "", e);
                throw (e);
            }
        }
    }

    @Test
    public void testWithNull() throws Exception {

        SimpleFeatureSource source = getFeatureSource(MockData.BASIC_POLYGONS);
        SimpleFeatureCollection fc = source.getFeatures();
        try (SimpleFeatureIterator i = fc.features()) {
            SimpleFeature f = i.next();

            FeatureTemplate template = new FeatureTemplate();
            template.description(f);

            // set a value to null
            f.setAttribute(1, null);
            try {
                template.description(f);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "", e);
                fail("template threw exception on null value");
            }
        }
    }

    @Test
    public void testAlternateLookup() throws Exception {
        SimpleFeatureSource source = getFeatureSource(MockData.PRIMITIVEGEOFEATURE);
        SimpleFeatureCollection fc = source.getFeatures();
        try (SimpleFeatureIterator features = fc.features()) {
            SimpleFeature f = features.next();

            FeatureTemplate template = new FeatureTemplate();
            String result = template.template(f, "dummy.ftl", Dummy.class);

            assertEquals("dummy", result);
        }
    }

    @Test
    public void testEmptyTemplate() throws Exception {
        SimpleFeatureSource source = getFeatureSource(MockData.PRIMITIVEGEOFEATURE);

        FeatureTemplate template = new FeatureTemplate();
        String defaultHeightTemplate;
        try (InputStream is = FeatureTemplate.class.getResourceAsStream("height.ftl")) {
            defaultHeightTemplate = IOUtils.toString(is, Charset.forName("UTF8"));
        }
        assertTrue(
                template.isTemplateEmpty(
                        source.getSchema(),
                        "height.ftl",
                        FeatureTemplate.class,
                        defaultHeightTemplate));
        assertTrue(
                template.isTemplateEmpty(
                        source.getSchema(), "time.ftl", FeatureTemplate.class, null));
        assertFalse(
                template.isTemplateEmpty(
                        source.getSchema(), "title.ftl", FeatureTemplate.class, null));
    }
}
