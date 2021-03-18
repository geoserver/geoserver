package org.geoserver.restconfig.model.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;
import org.geoserver.openapi.model.catalog.LayerInfo;
import org.geoserver.openapi.v1.model.Layer;
import org.geoserver.openapi.v1.model.Layer.TypeEnum;
import org.geoserver.openapi.v1.model.LayerResponse;
import org.geoserver.openapi.v1.model.LayerStyles;
import org.geoserver.openapi.v1.model.NamedLink;
import org.geoserver.restconfig.model.SerializationTest;
import org.junit.Test;

public class LayerResponseSerializationTest extends SerializationTest {

    public @Test void testLayerResponseCoverage() throws IOException {
        LayerResponse lrw =
                decode("LayerResponse_Coverage.json", LayerInfo.class, LayerResponse.class);
        assertNotNull(lrw);

        Layer response = lrw.getLayer();
        assertNotNull(response);
        assertEquals("PublishedName", response.getName());
        assertEquals(TypeEnum.RASTER, response.getType());
        assertNotNull(response.getDefaultStyle());
        assertEquals("raster", response.getDefaultStyle().getName());
        assertEquals(
                "http://localhost:8080/geoserver/rest/styles/raster.json",
                response.getDefaultStyle().getHref());

        assertNotNull(response.getResource());
        assertEquals("coverage", response.getResource().getAtClass());
        assertEquals("testWs:PublishedName", response.getResource().getName());
        assertEquals(
                "http://localhost:8080/geoserver/rest/workspaces/testWs/coveragestores/sfdem/coverages/PublishedName.json",
                response.getResource().getHref());

        assertEquals(Integer.valueOf(0), response.getAttribution().getLogoWidth());
        assertEquals(Integer.valueOf(0), response.getAttribution().getLogoHeight());
        // TODO
        // assertEquals("", response.getDateCreated());
    }

    public @Test void testLayerResponseFeatureType() throws IOException {
        LayerResponse lrw =
                decode("LayerResponse_FeatureType.json", LayerInfo.class, LayerResponse.class);
        assertNotNull(lrw);

        Layer response = lrw.getLayer();
        assertNotNull(response);
        assertEquals("streams", response.getName());
        assertEquals(TypeEnum.VECTOR, response.getType());
        assertEquals("/", response.getPath());
        assertNotNull(response.getDefaultStyle());
        assertEquals("simple_streams", response.getDefaultStyle().getName());
        assertEquals(
                "http://localhost:8080/geoserver/rest/styles/simple_streams.json",
                response.getDefaultStyle().getHref());

        assertNotNull(response.getResource());
        assertEquals("featureType", response.getResource().getAtClass());
        assertEquals("sf:streams", response.getResource().getName());
        assertEquals(
                "http://localhost:8080/geoserver/rest/workspaces/sf/datastores/sf/featuretypes/streams.json",
                response.getResource().getHref());

        assertEquals(Integer.valueOf(0), response.getAttribution().getLogoWidth());
        assertEquals(Integer.valueOf(0), response.getAttribution().getLogoHeight());

        LayerStyles styles = response.getStyles();
        List<NamedLink> style = styles.getStyle();
        NamedLink s = style.get(0);
        assertEquals("line", s.getName());
    }
}
