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
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geotools.data.DataTestCase;
import org.junit.Test;
import org.xml.sax.helpers.NamespaceSupport;

public class DynamicValueBuilderTest extends DataTestCase {

    @Test
    public void testEncodePath() throws IOException {
        JSONObject json = encodeDynamic("${id}");
        assertEquals(1, json.size());
        assertEquals("1", json.getString("key"));
    }

    @Test
    public void testEncodeNullPath() throws IOException {
        JSONObject json = encodeDynamic("${missingAttribute}");
        assertTrue(json.isEmpty());
    }

    @Test
    public void testEncodeNullCQL() throws IOException {
        JSONObject json = encodeDynamic("${a + b}");
        assertTrue(json.isEmpty());
    }

    private JSONObject encodeDynamic(String s) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GeoJSONWriter writer =
                new GeoJSONWriter(new JsonFactory().createGenerator(baos, JsonEncoding.UTF8));

        DynamicValueBuilder builder = new DynamicValueBuilder("key", s, new NamespaceSupport());
        writer.startObject();
        builder.evaluate(writer, new TemplateBuilderContext(roadFeatures[0]));
        writer.endObject();
        writer.close();

        // nothing has been encoded
        String jsonString = new String(baos.toByteArray());
        return (JSONObject) JSONSerializer.toJSON(jsonString);
    }
}
