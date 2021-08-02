/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.template;

import static org.junit.Assert.assertEquals;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import org.geotools.data.DataUtilities;
import org.geotools.data.complex.feature.type.ComplexFeatureTypeFactoryImpl;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.LenientFeatureFactoryImpl;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;

public class FeatureWrapperTest {
    DefaultFeatureCollection features;
    Configuration cfg;

    @Before
    public void setUp() throws Exception {

        // create some data
        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureType featureType =
                DataUtilities.createType(
                        "testType", "string:String,int:Integer,double:Double,geom:Point");

        features = new DefaultFeatureCollection() {};
        features.add(
                SimpleFeatureBuilder.build(
                        featureType,
                        new Object[] {
                            "one",
                            Integer.valueOf(1),
                            Double.valueOf(1.1),
                            gf.createPoint(new Coordinate(1, 1))
                        },
                        "fid.1"));
        features.add(
                SimpleFeatureBuilder.build(
                        featureType,
                        new Object[] {
                            "two",
                            Integer.valueOf(2),
                            Double.valueOf(2.2),
                            gf.createPoint(new Coordinate(2, 2))
                        },
                        "fid.2"));
        features.add(
                SimpleFeatureBuilder.build(
                        featureType,
                        new Object[] {
                            "three",
                            Integer.valueOf(3),
                            Double.valueOf(3.3),
                            gf.createPoint(new Coordinate(3, 3))
                        },
                        "fid.3"));
        cfg = TemplateUtils.getSafeConfiguration();
        cfg.setClassForTemplateLoading(getClass(), "");
        cfg.setObjectWrapper(createWrapper());
    }

    public FeatureWrapper createWrapper() {
        return new FeatureWrapper();
    }

    @Test
    public void testFeatureCollection() throws Exception {
        Template template = cfg.getTemplate("FeatureCollection.ftl");

        StringWriter out = new StringWriter();
        template.process(features, out);

        assertEquals(
                "fid.1\nfid.2\nfid.3\n",
                out.toString().replaceAll("\r\n", "\n").replaceAll("\r", "\n"));
    }

    @Test
    public void testFeatureSimple() throws Exception {
        Template template = cfg.getTemplate("FeatureSimple.ftl");

        StringWriter out = new StringWriter();
        template.process(features.iterator().next(), out);

        // replace ',' with '.' for locales which use a comma for decimal point
        assertEquals(
                "one\n1\n1.1\nPOINT (1 1)",
                out.toString().replace(',', '.').replaceAll("\r\n", "\n").replaceAll("\r", "\n"));
    }

    @Test
    public void testFeatureDynamic() throws Exception {
        Template template = cfg.getTemplate("FeatureDynamic.ftl");

        StringWriter out = new StringWriter();
        template.process(features.iterator().next(), out);

        // replace ',' with '.' for locales which use a comma for decimal point
        assertEquals(
                "string=one\nint=1\ndouble=1.1\ngeom=POINT (1 1)\n",
                out.toString().replace(',', '.').replaceAll("\r\n", "\n").replaceAll("\r", "\n"));
    }

    @Test
    public void testFeatureSequence() throws Exception {
        Template template = cfg.getTemplate("FeatureSequence.ftl");

        StringWriter out = new StringWriter();
        template.process(features, out);

        assertEquals(
                "three\none\n3", out.toString().replaceAll("\r\n", "\n").replaceAll("\r", "\n"));
    }

    @Test
    public void v() throws IOException, TemplateException {
        Feature f = buildComplexFeature();
        Template template = cfg.getTemplate("ComplexFeatureMaxOccurs.ftl");

        StringWriter out = new StringWriter();
        template.process(f, out);

        assertEquals(
                "\n"
                        + "Name: string_value\n"
                        + "Value: string1\n"
                        + "Name: string_value\n"
                        + "Value: string2\n",
                out.toString().replace(',', '.').replaceAll("\r\n", "\n").replaceAll("\r", "\n"));
    }

    private Feature buildComplexFeature() {
        AttributeTypeBuilder typeBuilder = new AttributeTypeBuilder();
        String nsURI = "http://namespace/test";
        typeBuilder
                .binding(String.class)
                .name("string_value")
                .namespaceURI(nsURI)
                .maxOccurs(2)
                .minOccurs(1);
        AttributeType attrType = typeBuilder.buildType();
        AttributeDescriptor attrDescriptor =
                typeBuilder.buildDescriptor(attrType.getName(), attrType);
        AttributeBuilder builder = new AttributeBuilder(new LenientFeatureFactoryImpl());
        builder.setDescriptor(attrDescriptor);
        ComplexFeatureTypeFactoryImpl ftBuilder = new ComplexFeatureTypeFactoryImpl();
        FeatureType complexType =
                ftBuilder.createFeatureType(
                        new NameImpl(nsURI, "ComplexUnboundedType"),
                        Arrays.asList(attrDescriptor),
                        null,
                        false,
                        null,
                        null,
                        null);
        ComplexFeatureBuilder complexFeatureBuilder = new ComplexFeatureBuilder(complexType);
        Attribute attribute1 = builder.buildSimple(null, "string1");
        Attribute attribute2 = builder.buildSimple(null, "string2");
        complexFeatureBuilder.append(attrDescriptor.getName(), attribute1);
        complexFeatureBuilder.append(attrDescriptor.getName(), attribute2);
        return complexFeatureBuilder.buildFeature("test.1");
    }
}
