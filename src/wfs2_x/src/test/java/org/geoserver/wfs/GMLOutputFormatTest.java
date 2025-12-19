package org.geoserver.wfs;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geotools.feature.NameImpl;
import org.geotools.wfs.v2_0.WFS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;

public class GMLOutputFormatTest extends WFSTestSupport {

    private int defaultNumDecimals = -1;
    private boolean defaultForceDecimal = false;
    private boolean defaultPadWithZeros = false;

    @Before
    public void saveDefaultFormattingOptions() {
        if (defaultNumDecimals < 0) {
            FeatureTypeInfo info = getGeoServer()
                    .getCatalog()
                    .getResourceByName(
                            new NameImpl(MockData.BASIC_POLYGONS.getPrefix(), MockData.BASIC_POLYGONS.getLocalPart()),
                            FeatureTypeInfo.class);
            defaultNumDecimals = info.getNumDecimals();
            defaultForceDecimal = info.getForcedDecimal();
            defaultPadWithZeros = info.getPadWithZeros();
        }
    }

    @After
    public void restoreDefaultFormattingOptions() {
        FeatureTypeInfo info = getGeoServer()
                .getCatalog()
                .getResourceByName(
                        new NameImpl(MockData.BASIC_POLYGONS.getPrefix(), MockData.BASIC_POLYGONS.getLocalPart()),
                        FeatureTypeInfo.class);
        info.setNumDecimals(defaultNumDecimals);
        info.setForcedDecimal(defaultForceDecimal);
        info.setPadWithZeros(defaultPadWithZeros);
    }

    // TODO fix these
    @Test
    public void testGML32CoordinatesFormatting() throws Exception {
        enableCoordinatesFormatting();
        Document dom = getAsDOM("wfs?request=getfeature&version=2.0.0&outputFormat=gml32&typename="
                + MockData.BASIC_POLYGONS.getPrefix()
                + ":"
                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals(
                "0.0000 -1.0000 1.0000 0.0000 0.0000 1.0000 -1.0000 0.0000 0.0000 -1.0000",
                dom.getElementsByTagName("gml:posList").item(0).getTextContent());
    }

    @Test
    public void testGML32() throws Exception {
        Document dom = getAsDOM("wfs?request=getfeature&version=2.0.0&outputFormat=gml32&typename="
                + MockData.BASIC_POLYGONS.getPrefix()
                + ":"
                + MockData.BASIC_POLYGONS.getLocalPart());
        assertEquals(WFS.NAMESPACE, dom.getDocumentElement().getNamespaceURI());
        assertEquals("FeatureCollection", dom.getDocumentElement().getLocalName());
    }

    private void enableCoordinatesFormatting() {
        FeatureTypeInfo info = getGeoServer()
                .getCatalog()
                .getResourceByName(
                        new NameImpl(MockData.BASIC_POLYGONS.getPrefix(), MockData.BASIC_POLYGONS.getLocalPart()),
                        FeatureTypeInfo.class);
        info.setNumDecimals(4);
        info.setForcedDecimal(true);
        info.setPadWithZeros(true);
        getGeoServer().getCatalog().save(info);
    }
}
