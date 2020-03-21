package com.boundlessgeo.gsr.api.map;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class GenerateKMLControllerTest extends WMSTestSupport {
    @Test
    public void generateKml() throws Exception {
        String exportMapUrl =
            "/gsr/services/" + SystemTestData.BASIC_POLYGONS.getPrefix() + "/MapServer/generateKml?layers="
                + SystemTestData.BASIC_POLYGONS.getLocalPart();

        String layerName = SystemTestData.BASIC_POLYGONS.getLocalPart();

        final XpathEngine xpath = XMLUnit.newXpathEngine();

        Document dom = getAsDOM(exportMapUrl);
        print(dom);
        assertXpathEvaluatesTo("1", "count(kml:kml/kml:Document)", dom);
        assertXpathEvaluatesTo("1", "count(kml:kml/kml:Document/kml:NetworkLink)", dom);
        assertXpathEvaluatesTo("1", "count(kml:kml/kml:Document/kml:LookAt)", dom);

        assertXpathEvaluatesTo(layerName, "kml:kml/kml:Document/kml:NetworkLink[1]/kml:name", dom);
        assertXpathEvaluatesTo("1", "kml:kml/kml:Document/kml:NetworkLink[1]/kml:open", dom);
        assertXpathEvaluatesTo("1", "kml:kml/kml:Document/kml:NetworkLink[1]/kml:visibility", dom);

        assertXpathEvaluatesTo("onStop", "kml:kml/kml:Document/kml:NetworkLink[1]/kml:Url/kml:viewRefreshMode", dom);
        assertXpathEvaluatesTo("1.0", "kml:kml/kml:Document/kml:NetworkLink[1]/kml:Url/kml:viewRefreshTime", dom);
        assertXpathEvaluatesTo("1.0", "kml:kml/kml:Document/kml:NetworkLink[1]/kml:Url/kml:viewBoundScale", dom);
        Map<String, Object> expectedKVP = KvpUtils.parseQueryString(
            "http://localhost:80/geoserver/wms?format_options=MODE%3Arefresh%3Bautofit%3Atrue%3BKMPLACEMARK%3Afalse"
                + "%3BKMATTR%3Atrue%3BKMSCORE%3A40%3BSUPEROVERLAY%3Afalse&service=wms&srs=EPSG%3A4326&width=2048"
                + "&styles=BasicPolygons&height=2048&transparent=false&request=GetMap&layers=cite%3ABasicPolygons"
                + "&format=application%2Fvnd.google-earth.kml+xml&version=1.1.1");
        Map<String, Object> resultedKVP = KvpUtils
            .parseQueryString(xpath.evaluate("kml:kml/kml:Document/kml:NetworkLink[1]/kml:Url/kml:href", dom));
    }

}