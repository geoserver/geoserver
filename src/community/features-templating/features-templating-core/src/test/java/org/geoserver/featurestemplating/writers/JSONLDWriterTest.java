package org.geoserver.featurestemplating.writers;

import static org.geoserver.featurestemplating.builders.VendorOptions.COLLECTION_NAME;
import static org.geoserver.featurestemplating.builders.VendorOptions.JSONLD_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.junit.Test;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

public class JSONLDWriterTest {

    @Test
    public void testJSONRootAttributesCollNameCustomization() throws IOException {
        EncodingHints encodingHints = new EncodingHints();
        encodingHints.put(COLLECTION_NAME, "diseaseSpreadStatistics");
        JSONObject json = writeJSONLD(encodingHints);
        assertEquals("FeatureCollection", json.getString(JSONLD_TYPE));
        assertTrue(json.has("diseaseSpreadStatistics"));
        assertFalse(json.has("features"));
    }

    @Test
    public void testJSONRootAttributesTypeCustomization() throws IOException {
        EncodingHints encodingHints = new EncodingHints();
        encodingHints.put(JSONLD_TYPE, "schema:PublicAnnouncement");
        JSONObject json = writeJSONLD(encodingHints);
        assertEquals("schema:PublicAnnouncement", json.getString(JSONLD_TYPE));
        assertTrue(json.has("features"));
    }

    @Test
    public void testJSONRootAttributesFullCustomization() throws IOException {
        EncodingHints encodingHints = new EncodingHints();
        encodingHints.put(JSONLD_TYPE, "schema:PublicAnnouncement");
        encodingHints.put(COLLECTION_NAME, "diseaseSpreadStatistics");
        JSONObject json = writeJSONLD(encodingHints);
        assertEquals("schema:PublicAnnouncement", json.getString(JSONLD_TYPE));
        assertTrue(json.has("diseaseSpreadStatistics"));
        assertFalse(json.has("features"));
    }

    @Test
    public void testJSONRootAttributesDefault() throws IOException {
        EncodingHints encodingHints = new EncodingHints();
        JSONObject json = writeJSONLD(encodingHints);
        assertEquals("FeatureCollection", json.getString(JSONLD_TYPE));
        assertTrue(json.has("features"));
    }

    private JSONObject writeJSONLD(EncodingHints hints) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JSONLDWriter writer = new JSONLDWriter(
                JsonFactory.builder().build().createGenerator(ObjectWriteContext.empty(), baos, JsonEncoding.UTF8));
        writer.startTemplateOutput(hints);
        writer.endTemplateOutput(hints);
        writer.close();
        String rawJSON = new String(baos.toByteArray(), Charset.forName("UTF-8"));
        return (JSONObject) JSONSerializer.toJSON(rawJSON);
    }
}
