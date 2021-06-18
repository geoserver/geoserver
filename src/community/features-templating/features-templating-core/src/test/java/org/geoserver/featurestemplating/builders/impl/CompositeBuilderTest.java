package org.geoserver.featurestemplating.builders.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geotools.data.DataTestCase;
import org.junit.Test;
import org.xml.sax.helpers.NamespaceSupport;

public class CompositeBuilderTest extends DataTestCase {

    @Test
    public void testCompositeNulls() throws Exception {
        NamespaceSupport ns = new NamespaceSupport();
        DynamicValueBuilder p1 = new DynamicValueBuilder("k1", "${notThere}", ns);
        DynamicValueBuilder p2 =
                new DynamicValueBuilder("k2", "$${strSubstring(notThereEither, 5, 10)}", ns);
        JSONObject json = buildComposite(ns, p1, p2);

        assertTrue(json.isEmpty());
    }

    @Test
    public void testCompositeOneKey() throws Exception {
        NamespaceSupport ns = new NamespaceSupport();
        DynamicValueBuilder p1 = new DynamicValueBuilder("k1", "${id}", ns);
        DynamicValueBuilder p2 = new DynamicValueBuilder("k2", "$${strToUpperCase(notThere)}", ns);
        JSONObject json = buildComposite(ns, p1, p2);

        assertFalse(json.isEmpty());
        assertEquals(1, json.size());
        JSONObject composite = json.getJSONObject("composite");
        assertEquals(Integer.valueOf(1), composite.get("k1"));
        assertFalse(json.has("k2"));
    }

    @Test
    public void testCompositeTwoKeys() throws Exception {
        NamespaceSupport ns = new NamespaceSupport();
        DynamicValueBuilder p1 = new DynamicValueBuilder("k1", "${id}", ns);
        DynamicValueBuilder p2 = new DynamicValueBuilder("k2", "$${strToUpperCase(name)}", ns);
        JSONObject json = buildComposite(ns, p1, p2);
        System.out.println(json.toString());

        assertFalse(json.isEmpty());
        assertEquals(1, json.size());
        JSONObject composite = json.getJSONObject("composite");
        assertEquals(Integer.valueOf(1), composite.get("k1"));
        assertEquals("R1", composite.get("k2"));
    }

    private JSONObject buildComposite(
            NamespaceSupport ns, DynamicValueBuilder p1, DynamicValueBuilder p2)
            throws IOException {
        CompositeBuilder composite = new CompositeBuilder("composite", ns, false);
        composite.addChild(p1);
        composite.addChild(p2);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GeoJSONWriter writer =
                new GeoJSONWriter(
                        new JsonFactory().createGenerator(baos, JsonEncoding.UTF8),
                        TemplateIdentifier.JSON);

        writer.writeStartObject();
        composite.evaluate(writer, new TemplateBuilderContext(roadFeatures[0]));
        writer.writeEndObject();
        writer.close();

        // nothing has been encoded
        String jsonString = new String(baos.toByteArray());
        return (JSONObject) JSONSerializer.toJSON(jsonString);
    }
}
