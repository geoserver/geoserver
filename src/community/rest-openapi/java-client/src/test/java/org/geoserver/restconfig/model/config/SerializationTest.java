package org.geoserver.restconfig.model.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import feign.jackson.JacksonDecoder;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import org.apache.commons.codec.Charsets;
import org.geoserver.openapi.model.config.GmlInfo;
import org.geoserver.openapi.model.config.GmlSettings;
import org.geoserver.openapi.model.config.SrsNameStyle;
import org.geoserver.openapi.model.config.WFSInfo;
import org.geoserver.openapi.model.config.WFSInfoGmlSettings;
import org.geoserver.openapi.model.config.WMSInfo;
import org.geoserver.openapi.v1.model.WFSInfoWrapper;
import org.geoserver.openapi.v1.model.WMSInfoWrapper;
import org.geoserver.restconfig.client.GeoServerClient;
import org.junit.Before;
import org.junit.Test;

public class SerializationTest {

    private static final String SERVICE_INFO =
            "{" //
                    + "\"workspace\":{\"name\":\"topp\"}," //
                    + "\"enabled\":true," //
                    + "\"name\":\"My GeoServer\"," //
                    + "\"title\":\"Test Title\"," //
                    + "\"maintainer\":\"http:\\/\\/geoserver.org\\/comm\"," //
                    + "\"abstrct\":\"This is a description of your OWS.\"," //
                    + "\"accessConstraints\":\"NONE\"," //
                    + "\"fees\":\"NONE\"," //
                    + "\"versions\":{\"org.geotools.util.Version\":[{\"version\":\"1.1.1\"},{\"version\":\"1.3.0\"}]}," //
                    + "\"keywords\":{\"string\":[\"WFS\",\"WMS\",\"GEOSERVER\",\"Hola\\\\@language=es\\\\;\",\"Hola\\\\@vocabulary=Vocabulary\\\\;\"]}," //
                    + "\"citeCompliant\":false," //
                    + "\"onlineResource\":\"http:\\/\\/geoserver.org\"," //
                    + "\"schemaBaseURL\":\"http:\\/\\/schemas.opengis.net\"," //
                    + "\"verbose\":true," //
                    + "\"metadata\":{\"entry\":[{\"@key\":\"disableDatelineWrappingHeuristic\",\"$\":\"false\"},{\"@key\":\"kmlSuperoverlayMode\",\"$\":\"auto\"},{\"@key\":\"kmlReflectorMode\",\"$\":\"refresh\"},{\"@key\":\"loopContinuously\",\"$\":\"false\"},{\"@key\":\"svgAntiAlias\",\"$\":\"true\"},{\"@key\":\"kmlPlacemark\",\"$\":\"false\"},{\"@key\":\"kmlKmscore\",\"$\":\"40\"},{\"@key\":\"mapWrapping\",\"$\":\"true\"},{\"@key\":\"pngCompression\",\"$\":\"25\"},{\"@key\":\"jpegCompression\",\"$\":\"25\"},{\"@key\":\"advancedProjectionDensification\",\"$\":\"false\"},{\"@key\":\"advancedProjectionHandling\",\"$\":\"true\"},{\"@key\":\"maxAllowedFrames\",\"$\":\"2147483647\"},{\"@key\":\"kmlAttr\",\"$\":\"true\"},{\"@key\":\"svgRenderer\",\"$\":\"Batik\"},{\"@key\":\"disposalMethod\",\"$\":\"none\"},{\"@key\":\"scalehintMapunitsPixel\",\"$\":\"false\"},{\"@key\":\"framesDelay\",\"$\":\"1000\"}]}," //
                    + "${serviceInfo}" //
                    + "}"; //

    private static final String WMS_INFO =
            SERVICE_INFO.replace(
                    "${serviceInfo}", //
                    "\"srs\":{\"string\":[4326,3857]}," //
                            + "\"bboxForEachCRS\":false," //
                            + "\"watermark\":{\"enabled\":false,\"position\":\"BOT_RIGHT\",\"transparency\":0}," //
                            + "\"interpolation\":\"Nearest\"," //
                            + "\"getFeatureInfoMimeTypeCheckingEnabled\":false," //
                            + "\"getMapMimeTypeCheckingEnabled\":false," //
                            + "\"dynamicStylingDisabled\":false," //
                            + "\"featuresReprojectionDisabled\":false," //
                            + "\"authorityURLs\":{\"AuthorityURL\":[{\"name\":\"CITE\",\"href\":\"http:\\/\\/www.opengeospatial.org\\/cite\"}]}," //
                            + "\"identifiers\":{\"Identifier\":[{\"authority\":\"CITE\",\"identifier\":\"root\"}]}," //
                            + "\"maxBuffer\":25," //
                            + "\"maxRequestMemory\":0," //
                            + "\"maxRenderingTime\":0," //
                            + "\"maxRenderingErrors\":0," //
                            + "\"rootLayerTitle\":\"Root Layer Title\"," //
                            + "\"rootLayerAbstract\":\"Root Layer abstract\"," //
                            + "\"maxRequestedDimensionValues\":100," //
                            + "\"cacheConfiguration\":{\"enabled\":false,\"maxEntries\":1000,\"maxEntrySize\":51200}");

    private static final String WFS_INFO =
            SERVICE_INFO.replace(
                    "${serviceInfo}", //
                    "\"gml\":{\"entry\":[" //
                            + "{\"version\":\"V_11\",\"gml\":{\"srsNameStyle\":[\"URN\"],\"overrideGMLAttributes\":false}}," //
                            + "{\"version\":\"V_20\",\"gml\":{\"srsNameStyle\":[\"URN2\"],\"overrideGMLAttributes\":false}}," //
                            + "{\"version\":\"V_10\",\"gml\":{\"srsNameStyle\":[\"XML\"],\"overrideGMLAttributes\":true}}" //
                            + "]}," //
                            + "\"serviceLevel\":\"COMPLETE\"," //
                            + "\"maxFeatures\":1000000," //
                            + "\"featureBounding\":false," //
                            + "\"canonicalSchemaLocation\":false," //
                            + "\"encodeFeatureMember\":false," //
                            + "\"hitsIgnoreMaxFeatures\":false," //
                            + "\"srs\":{\"string\":[4326,3857,3003]}"); //

    private ObjectMapper objectMapper;
    private JacksonDecoder decoder;

    public @Before void before() {
        // mimic ApiClient's initialization of feign encoder/decoder
        objectMapper = GeoServerClient.newApiClient().getObjectMapper();
        decoder = new JacksonDecoder(objectMapper);
    }

    private <T> T decode(String raw, Type type) throws IOException {
        @SuppressWarnings("deprecation")
        Request request =
                Request.create("GET", "http://doesntmatter", Collections.emptyMap(), null, null);
        Response response = Response.builder().request(request).body(raw, Charsets.UTF_8).build();
        @SuppressWarnings("unchecked")
        T decoded = (T) decoder.decode(response, type);
        return decoded;
    }

    public @Test void testWMSInfo() throws IOException {
        WMSInfo info = decode(WMS_INFO, WMSInfo.class);
        assertNotNull(info);
        assertWMSInfo(info);
    }

    public @Test void testWMSInfoWrapper() throws IOException {
        String raw = String.format("{\"wms\":%s}", WMS_INFO);
        WMSInfoWrapper wrapper = decode(raw, WMSInfoWrapper.class);
        assertNotNull(wrapper);
        assertNotNull(wrapper.getWms());
        assertWMSInfo(wrapper.getWms());
    }

    private void assertWMSInfo(WMSInfo info) {
        assertEquals("topp", info.getWorkspace().getName());
        assertEquals("My GeoServer", info.getName());
        assertEquals("Test Title", info.getTitle());
        assertEquals("This is a description of your OWS.", info.getAbstrct());
    }

    public @Test void testGmlInfo() throws IOException {
        final String raw =
                "{\"entry\":[" //
                        + "{\"version\":\"V_10\",\"gml\":{\"srsNameStyle\":[\"XML\"],\"overrideGMLAttributes\":true}}," //
                        + "{\"version\":\"V_11\",\"gml\":{\"srsNameStyle\":[\"URN\"],\"overrideGMLAttributes\":false}}," //
                        + "{\"version\":\"V_20\",\"gml\":{\"srsNameStyle\":[\"URN2\"],\"overrideGMLAttributes\":false}}" //
                        + "]}"; //
        WFSInfoGmlSettings gml = decode(raw, WFSInfoGmlSettings.class);
        assertNotNull(gml);
        List<GmlInfo> entry = gml.getEntry();
        assertEquals(3, entry.size());
        assertGmlInfo(entry.get(0), "V_10", SrsNameStyle.XML, true);
        assertGmlInfo(entry.get(1), "V_11", SrsNameStyle.URN, false);
        assertGmlInfo(entry.get(2), "V_20", SrsNameStyle.URN2, false);
    }

    private void assertGmlInfo(
            GmlInfo entry,
            String version,
            SrsNameStyle srsNameStyle,
            boolean overrideGMLAttributes) {
        GmlInfo expected;
        expected =
                new GmlInfo()
                        .version(version)
                        .gml(
                                new GmlSettings()
                                        .addSrsNameStyleItem(srsNameStyle)
                                        .overrideGMLAttributes(overrideGMLAttributes));
        assertEquals(expected, entry);
    }

    public @Test void testWFSInfo() throws IOException {
        WFSInfo info = decode(WFS_INFO, WFSInfo.class);
        assertNotNull(info);
        assertWFSInfo(info);
    }

    public @Test void testWFSInfoWrapper() throws IOException {
        String raw = String.format("{\"wfs\":%s}", WFS_INFO);
        WFSInfoWrapper wrapper = decode(raw, WFSInfoWrapper.class);
        assertNotNull(wrapper);
        assertNotNull(wrapper.getWfs());
        assertWFSInfo(wrapper.getWfs());
    }

    private void assertWFSInfo(WFSInfo info) {
        assertEquals("topp", info.getWorkspace().getName());
        assertEquals("My GeoServer", info.getName());
        assertEquals("Test Title", info.getTitle());
        assertEquals("This is a description of your OWS.", info.getAbstrct());

        WFSInfoGmlSettings gml = info.getGml();
        List<GmlInfo> entry = gml.getEntry();
        assertEquals(3, entry.size());
        assertGmlInfo(entry.get(0), "V_11", SrsNameStyle.URN, false);
        assertGmlInfo(entry.get(1), "V_20", SrsNameStyle.URN2, false);
        assertGmlInfo(entry.get(2), "V_10", SrsNameStyle.XML, true);
    }
}
