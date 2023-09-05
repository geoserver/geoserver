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
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import net.sf.json.*;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GMLTemplateWriter;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geotools.api.feature.Attribute;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
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
import org.xml.sax.helpers.NamespaceSupport;

public class DynamicValueBuilderTest extends DataTestCase {

    private static final String JSON_PAYLOAD = "{ \"a\": 1, \"b\": [1, 2, 3]}";
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

    public List<SimpleFeature> setFeaturesWithNullable() {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.add("nullAttribute", String.class);
        tb.setName("nullType");
        SimpleFeatureType schema = tb.buildFeatureType();

        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        fb.add(null);
        SimpleFeature f1 = fb.buildFeature("nullType.1");
        fb.add("notNullValue");
        SimpleFeature f2 = fb.buildFeature("nullType.2");
        return Arrays.asList(f1, f2);
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
        JSONObject json = encodeDynamic("${id}", roadFeatures[0]);
        assertEquals(1, json.size());
        assertEquals("1", json.getString("key"));
    }

    @Test
    public void testEncodeNullPath() throws IOException {
        JSONObject json = encodeDynamic("${missingAttribute}", roadFeatures[0]);
        assertTrue(json.isEmpty());
    }

    @Test
    public void testEncodeNullCQL() throws IOException {
        JSONObject json = encodeDynamic("${a + b}", roadFeatures[0]);
        assertTrue(json.isEmpty());
    }

    @Test
    public void testJSONXPathSimpleFeature() throws Exception {
        JSONObject json = encodeDynamic("${jf}", jsonFieldSimpleFeature);
        JSONObject obj = json.getJSONObject("key");
        assertEquals(1, obj.getInt("a"));
        assertEquals(Arrays.asList(1, 2, 3), obj.getJSONArray("b"));
    }

    @Test
    public void testJSONXPathEncodeNull() throws Exception {
        List<SimpleFeature> features = setFeaturesWithNullable();
        JSONObject json = encodeDynamic("${nullAttribute}!", features.get(0));
        assertTrue(json.get("key") instanceof JSONNull);
        json = encodeDynamic("${nullAttribute}!", features.get(1));
        assertEquals(json.getString("key"), "notNullValue");
    }

    @Test
    public void testXMLXPathEncodeNull() throws Exception {
        List<SimpleFeature> features = setFeaturesWithNullable();
        String xml = encodeXML("${nullAttribute}!", features.get(0));
        assertEquals(xml, "<key></key>");
        xml = encodeXML("${nullAttribute}!", features.get(1));
        assertEquals(xml, "<key>notNullValue</key>");
    }

    @Test
    public void testJSONXPathComplexFeature() throws Exception {
        JSONObject json = encodeDynamic("${jf}", jsonFieldComplexFeature);
        JSONObject obj = json.getJSONObject("key");
        assertEquals(1, obj.getInt("a"));
        assertEquals(Arrays.asList(1, 2, 3), obj.getJSONArray("b"));
    }

    @Test
    public void testJSONCQLSimpleFeature() throws Exception {
        JSONObject json = encodeDynamic("$${jf}", jsonFieldSimpleFeature);
        JSONObject obj = json.getJSONObject("key");
        assertEquals(1, obj.getInt("a"));
        assertEquals(Arrays.asList(1, 2, 3), obj.getJSONArray("b"));
    }

    @Test
    public void testJSONCQLComplexFeature() throws Exception {
        JSONObject json = encodeDynamic("$${jf}", jsonFieldComplexFeature);
        JSONObject obj = json.getJSONObject("key");
        assertEquals(1, obj.getInt("a"));
        assertEquals(Arrays.asList(1, 2, 3), obj.getJSONArray("b"));
    }

    @Test
    public void testJSONPointer() throws Exception {
        JSONObject json = encodeDynamic("$${jsonPointer(jf, '/b')}", jsonFieldComplexFeature);
        JSONArray obj = json.getJSONArray("key");
        assertEquals(Arrays.asList(1, 2, 3), obj);
    }

    private JSONObject encodeDynamic(String expression, Feature feature) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GeoJSONWriter writer =
                new GeoJSONWriter(
                        new JsonFactory().createGenerator(baos, JsonEncoding.UTF8),
                        TemplateIdentifier.JSON);

        DynamicValueBuilder builder =
                new DynamicValueBuilder("key", expression, new NamespaceSupport());

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
