/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import it.geosolutions.imageio.pam.PAMDataset;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand;
import it.geosolutions.imageio.pam.PAMParser;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;

/** Unit tests for RasterAttributeTableVisitor */
public class AttributeTableEnricherTest {

    private static final double EPS = 1e-6;
    static File ROOT = new File("./src/test/resources/org/geoserver/wms/featureinfo");

    @Test
    public void testRanges() throws Exception {
        PAMParser parser = new PAMParser();
        PAMDataset pam = parser.parsePAM(new File(ROOT, "rat.tiff.aux.xml"));
        PAMRasterBand band = pam.getPAMRasterBand().get(0);

        // expand attributes
        AttributeTableEnricher enricher = new AttributeTableEnricher(band);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("test");
        tb.add("gray", Integer.class);
        enricher.addAttributes(tb);
        SimpleFeatureType schema = tb.buildFeatureType();
        assertEquals(4, schema.getAttributeCount());
        assertEquals(Integer.class, schema.getDescriptor("gray").getType().getBinding());
        assertEquals(Double.class, schema.getDescriptor("con_min").getType().getBinding());
        assertEquals(Double.class, schema.getDescriptor("con_max").getType().getBinding());
        assertEquals(String.class, schema.getDescriptor("test").getType().getBinding());

        // map attributes for 1.1
        List<Object> values = new ArrayList<>();
        enricher.addRowValues(values, new double[] {1.1});
        assertEquals(1d, (Double) values.get(0), EPS);
        assertEquals(1.2d, (Double) values.get(1), EPS);
        assertEquals("green", values.get(2));

        // same but for 12.1
        values = new ArrayList<>();
        enricher.addRowValues(values, new double[] {12.1});
        assertEquals(12.1d, (Double) values.get(0), EPS);
        assertEquals(12.3d, (Double) values.get(1), EPS);
        assertEquals("purple", values.get(2));

        // look up a case that is not a match
        values = new ArrayList<>();
        enricher.addRowValues(values, new double[] {1e6});
        assertEquals(3, values.size());
        for (int i = 0; i < 3; i++) {
            assertNull(values.get(i));
        }
    }

    @Test
    public void testExactMatch() throws Exception {
        PAMParser parser = new PAMParser();
        PAMDataset pam = parser.parsePAM(new File(ROOT, "rat_int.xml"));
        PAMRasterBand band = pam.getPAMRasterBand().get(0);

        // expand attributes
        AttributeTableEnricher enricher = new AttributeTableEnricher(band);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("test");
        tb.add("gray", Integer.class);
        enricher.addAttributes(tb);
        SimpleFeatureType schema = tb.buildFeatureType();
        assertEquals(9, schema.getAttributeCount());
        assertEquals(Long.class, schema.getDescriptor("Value").getType().getBinding());
        assertEquals(Long.class, schema.getDescriptor("Count").getType().getBinding());
        assertEquals(String.class, schema.getDescriptor("Class").getType().getBinding());
        assertEquals(String.class, schema.getDescriptor("Class2").getType().getBinding());
        assertEquals(String.class, schema.getDescriptor("Class3").getType().getBinding());
        assertEquals(Long.class, schema.getDescriptor("Red").getType().getBinding());
        assertEquals(Long.class, schema.getDescriptor("Green").getType().getBinding());
        assertEquals(Long.class, schema.getDescriptor("Blue").getType().getBinding());

        // map attributes for 4
        List<Object> values = new ArrayList<>();
        enricher.addRowValues(values, new double[] {4});
        assertEquals(4l, values.get(0));
        assertEquals(2l, values.get(1));
        assertEquals("two", values.get(2));
        assertEquals("two2", values.get(3));
        assertEquals("two3", values.get(4));
        assertEquals(200l, values.get(5));
        assertEquals(30l, values.get(6));
        assertEquals(50l, values.get(7));

        // look up a case that is not a match
        values = new ArrayList<>();
        enricher.addRowValues(values, new double[] {-10});
        assertEquals(8, values.size());
        for (int i = 0; i < 8; i++) {
            assertNull(values.get(i));
        }
    }

    @Test
    public void testGdal312Types() throws Exception {
        PAMParser parser = new PAMParser();
        PAMDataset pam = parser.parsePAM(new File(ROOT, "rat_gdal312.xml"));
        PAMRasterBand band = pam.getPAMRasterBand().get(0);

        AttributeTableEnricher enricher = new AttributeTableEnricher(band);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("test");
        tb.add("gray", Integer.class);
        enricher.addAttributes(tb);
        SimpleFeatureType schema = tb.buildFeatureType();
        assertEquals(9, schema.getAttributeCount());
        assertEquals(Long.class, schema.getDescriptor("id").getType().getBinding());
        assertEquals(
                Long.class, schema.getDescriptor("dataAssessment").getType().getBinding());
        assertEquals(
                Boolean.class,
                schema.getDescriptor("fullSeafloorCoverageAchieved").getType().getBinding());
        assertEquals(
                Date.class,
                schema.getDescriptor("surveyDateRange.dateStart").getType().getBinding());
        assertEquals(
                Date.class,
                schema.getDescriptor("surveyDateRange.dateEnd").getType().getBinding());
        assertEquals(
                String.class, schema.getDescriptor("surveyAuthority").getType().getBinding());
        assertEquals(
                Double.class,
                schema.getDescriptor("depthRange.minimumDepth").getType().getBinding());
        // a geometry stays text, a geometry attribute would become the default geometry
        assertEquals(String.class, schema.getDescriptor("footprint").getType().getBinding());

        // the row whose dates carry a -05:00 offset, so the instant is not what the text reads
        List<Object> values = new ArrayList<>();
        enricher.addRowValues(values, new double[] {54603});
        assertEquals(8, values.size());
        assertEquals(54603l, values.get(0));
        assertEquals(2l, values.get(1));
        assertEquals(Boolean.FALSE, values.get(2));
        assertEquals(Date.from(Instant.parse("2024-03-01T04:00:00Z")), values.get(3));
        assertEquals(Date.from(Instant.parse("2024-03-16T11:00:00Z")), values.get(4));
        assertEquals("NOAA", values.get(5));
        assertEquals(12.5d, (Double) values.get(6), EPS);
        assertEquals("POLYGON ((1 0,2 0,2 1,1 1,1 0))", values.get(7));

        // the row where only the survey id is set: GDAL writes zeros and empty elements
        values = new ArrayList<>();
        enricher.addRowValues(values, new double[] {61004});
        assertEquals(8, values.size());
        assertEquals(61004l, values.get(0));
        assertEquals(0l, values.get(1));
        assertEquals(Boolean.FALSE, values.get(2));
        assertNull(values.get(3));
        assertNull(values.get(4));
        assertEquals("", values.get(5));
        assertEquals(0d, (Double) values.get(6), EPS);
        assertEquals("", values.get(7));
    }
}
