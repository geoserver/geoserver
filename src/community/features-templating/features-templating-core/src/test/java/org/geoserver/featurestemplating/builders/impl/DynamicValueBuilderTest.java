/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2021, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.featurestemplating.builders.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GMLTemplateWriter;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geotools.data.DataTestCase;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.TypeBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.xml.sax.helpers.NamespaceSupport;

public class DynamicValueBuilderTest extends DataTestCase {

    private static final String JSON_PAYLOAD = "{ \"a\": 1, \"c\": null, \"b\": [1, 2, 3]}";
    private SimpleFeature jsonFieldSimpleFeature;
    private Feature jsonFieldComplexFeature;

    @Before
    public void setupJSONFieldSimpleFeature() {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.userData(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        tb.add("jf", String.class);
        tb.setName("jsonFieldSimpleType");
        SimpleFeatureType schema = tb.buildFeatureType();

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        fb.add(JSON_PAYLOAD);
        jsonFieldSimpleFeature = fb.buildFeature("jsonFieldSimpleType.1");
    }

    public SimpleFeature setupNullSimpleFeature() {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.userData(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        tb.add("nullAttribute", String.class);
        tb.setName("nullFeatureType");
        SimpleFeatureType schema = tb.buildFeatureType();

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        fb.add(null);
        return fb.buildFeature("nullType.1");
    }

    @Before
    public void setupJSONFieldComplexFeature() {

        TypeBuilder typeBuilder = new TypeBuilder(CommonFactoryFinder.getFeatureTypeFactory(null));
        AttributeTypeBuilder atb = new AttributeTypeBuilder();
        atb.name("jf");
        atb.setBinding(String.class);
        atb.userData(JDBCDataStore.JDBC_NATIVE_TYPENAME, "json");
        AttributeDescriptor ad = atb.buildDescriptor("jf");

        typeBuilder.add(ad);
        typeBuilder.setName("jsonFieldComplexType");
        FeatureType schema = typeBuilder.feature();

        ComplexFeatureBuilder fb = new ComplexFeatureBuilder(schema);
        AttributeBuilder ab = new AttributeBuilder(CommonFactoryFinder.getFeatureFactory(null));
        ab.setDescriptor(ad);
        Attribute attribute = ab.buildSimple(null, JSON_PAYLOAD);
        fb.append(ad.getName(), attribute);
        this.jsonFieldComplexFeature = fb.buildFeature("jsonFieldComplexType.1");
    }

    @Test
    public void testEncodePath() throws IOException {
        JSONObject json = encodeDynamic("${id}", roadFeatures[0], false);
        assertEquals(1, json.size());
        assertEquals("1", json.getString("key"));
    }

    @Test
    public void testEncodeNullPath() throws IOException {
        JSONObject json = encodeDynamic("${missingAttribute}", roadFeatures[0], false);
        assertTrue(json.isEmpty());
    }

    @Test
    public void testEncodeNullCQL() throws IOException {
        JSONObject json = encodeDynamic("${a + b}", roadFeatures[0], false);
        assertTrue(json.isEmpty());
    }

    @Test
    public void testJSONXPathSimpleFeature() throws Exception {
        JSONObject json = encodeDynamic("${jf}", jsonFieldSimpleFeature, false);
        JSONObject obj = json.getJSONObject("key");
        assertEquals(1, obj.getInt("a"));
        assertEquals(Arrays.asList(1, 2, 3), obj.getJSONArray("b"));
    }

    @Test
    public void testJSONXPathEncodeNull() throws Exception {
        JSONObject json = encodeDynamic("${jf}", jsonFieldSimpleFeature, true);
        JSONObject obj = json.getJSONObject("key");
        assertEquals("null", obj.get("c").toString());
        assertEquals(JSONNull.class, obj.get("c").getClass());
    }

    @Test
    public void testXMLXPathEncodeNull() throws Exception {
        SimpleFeature nullFeature = setupNullSimpleFeature();
        String xml = encodeXML("${nullAttribute}!", nullFeature);
        assertEquals(xml, "<key></key>");
    }

    @Test
    public void testJSONXPathComplexFeature() throws Exception {
        JSONObject json = encodeDynamic("${jf}", jsonFieldComplexFeature, false);
        JSONObject obj = json.getJSONObject("key");
        assertEquals(1, obj.getInt("a"));
        assertEquals(Arrays.asList(1, 2, 3), obj.getJSONArray("b"));
    }

    @Test
    public void testJSONCQLSimpleFeature() throws Exception {
        JSONObject json = encodeDynamic("$${jf}", jsonFieldSimpleFeature, false);
        JSONObject obj = json.getJSONObject("key");
        assertEquals(1, obj.getInt("a"));
        assertEquals(Arrays.asList(1, 2, 3), obj.getJSONArray("b"));
    }

    @Test
    public void testJSONCQLComplexFeature() throws Exception {
        JSONObject json = encodeDynamic("$${jf}", jsonFieldComplexFeature, false);
        JSONObject obj = json.getJSONObject("key");
        assertEquals(1, obj.getInt("a"));
        assertEquals(Arrays.asList(1, 2, 3), obj.getJSONArray("b"));
    }

    @Test
    public void testJSONPointer() throws Exception {
        JSONObject json =
                encodeDynamic("$${jsonPointer(jf, '/b')}", jsonFieldComplexFeature, false);
        JSONArray obj = json.getJSONArray("key");
        assertEquals(Arrays.asList(1, 2, 3), obj);
    }

    private JSONObject encodeDynamic(String expression, Feature feature, boolean encodeNull)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GeoJSONWriter writer =
                new GeoJSONWriter(
                        new JsonFactory().createGenerator(baos, JsonEncoding.UTF8),
                        TemplateIdentifier.JSON);

        DynamicValueBuilder builder =
                new DynamicValueBuilder("key", expression, new NamespaceSupport());
        if (encodeNull) {
            builder.addEncodingHint("ENCODE_NULL", true);
        }

        writer.writeStartObject();
        builder.evaluate(writer, new TemplateBuilderContext(feature));
        writer.writeEndObject();
        writer.close();

        // nothing has been encoded
        String jsonString = new String(baos.toByteArray());
        return (JSONObject) JSONSerializer.toJSON(jsonString);
    }

    private String encodeXML(String expression, Feature feature) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GMLTemplateWriter outputWriter = getGmlWriter(TemplateIdentifier.GML31, baos);

        DynamicValueBuilder builder =
                new DynamicValueBuilder("key", expression, new NamespaceSupport());
        builder.addEncodingHint("ENCODE_NULL", true);
        builder.evaluate(outputWriter, new TemplateBuilderContext(feature));
        outputWriter.close();

        // nothing has been encoded
        return new String(baos.toByteArray());
    }

    private GMLTemplateWriter getGmlWriter(TemplateIdentifier identifier, OutputStream out)
            throws XMLStreamException {
        XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter xMLStreamWriter = xMLOutputFactory.createXMLStreamWriter(out);
        GMLTemplateWriter outputWriter =
                new GMLTemplateWriter(xMLStreamWriter, identifier.getOutputFormat());
        outputWriter.addNamespaces(new HashMap<>());
        return outputWriter;
    }
}
