/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.junit.Assert.*;

import org.geoserver.wms.WMSInfo;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class WmsGetFeatureInfoTest extends AbstractAppSchemaTestSupport {

    public WmsGetFeatureInfoTest() throws Exception {
        super();
    }

    @Before
    public void setupAdvancedProjectionHandling() {
        // make sure GetFeatureInfo is not deactivated (this will only update the global service)
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(false);
        getGeoServer().save(wms);
    }

    @Override
    protected WmsSupportMockData createTestData() {
        WmsSupportMockData mockData = new WmsSupportMockData();
        mockData.addStyle("Default", "styles/Default.sld");
        mockData.addStyle("positionalaccuracy21", "styles/positionalaccuracy21.sld");
        return mockData;
    }

    @Test
    public void testGetCapabilities() {
        Document doc = getAsDOM("wms?request=GetCapabilities");
        LOGGER.info("WMS =GetCapabilities response:\n" + prettyString(doc));
        assertEquals("WMS_Capabilities", doc.getDocumentElement().getNodeName());
        assertXpathCount(1, "//wms:Layer/wms:Name[.='gsml:MappedFeature']", doc);
        assertXpathCount(
                1, "//wms:GetFeatureInfo/wms:Format[.='application/vnd.ogc.gml/3.1.1']", doc);
    }

    @Test
    public void testGetFeatureInfoText() throws Exception {
        String str =
                getAsString(
                        "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100");
        LOGGER.info("WMS =GetFeatureInfo Text response:\n" + str);
        assertTrue(str.contains("FeatureImpl:MappedFeature<MappedFeatureType id=mf2>"));
    }

    @Test
    public void testGetFeatureInfoGML() throws Exception {
        String request =
                "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=application/vnd.ogc.gml/3.1.1";
        Document doc = getAsDOM(request);
        LOGGER.info("WMS =GetFeatureInfo GML response:\n" + prettyString(doc));
        assertXpathCount(1, "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature", doc);
        assertXpathEvaluatesTo(
                "mf2", "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/@gml:id", doc);
        assertXpathEvaluatesTo(
                "gu.25678",
                "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        validateGet(request);
    }

    @Test
    public void testGetFeatureInfoGMLReprojection() throws Exception {
        String request =
                "wms?request=GetFeatureInfo&SRS=EPSG:3857&BBOX=-144715.338031256,6800125.45439731,0,6891041.72389159&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=application/vnd.ogc.gml/3.1.1";
        Document doc = getAsDOM(request);
        LOGGER.info("WMS =GetFeatureInfo GML response:\n" + prettyString(doc));
        assertXpathCount(1, "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature", doc);
        assertXpathEvaluatesTo(
                "mf2", "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/@gml:id", doc);
        assertXpathEvaluatesTo(
                "gu.25678",
                "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        // check that features coordinates where reprojected to EPSG:3857
        assertXpathMatches(
                ".*3857",
                "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature[@gml:id='mf2']/gsml:shape/gml:Polygon/@srsName",
                doc);
        validateGet(request);
        // disable features reprojection
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(true);
        getGeoServer().save(wms);
        // execute the request
        doc = getAsDOM(request);
        // check that features were not reprojected and still in EPSG:4326
        assertXpathMatches(
                ".*4326",
                "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature[@gml:id='mf2']/gsml:shape/gml:Polygon/@srsName",
                doc);
    }

    @Test
    public void testGetFeatureInfoGML21() throws Exception {
        String request =
                "wms?request=GetFeatureInfo&styles=positionalaccuracy21&SRS=EPSG:4326&BBOX=-1.3,53,0,53.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=application/vnd.ogc.gml/3.1.1";
        Document doc = getAsDOM(request);
        LOGGER.info("WMS =GetFeatureInfo GML response:\n" + prettyString(doc));
        assertXpathCount(1, "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature", doc);
        assertXpathEvaluatesTo(
                "mf4", "/wfs:FeatureCollection/gml:featureMember/gsml:MappedFeature/@gml:id", doc);
        validateGet(request);
    }

    @Test
    public void testGetFeatureInfoHTML() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=GetFeatureInfo&SRS=EPSG:4326&BBOX=-1.3,52,0,52.5&LAYERS=gsml:MappedFeature&QUERY_LAYERS=gsml:MappedFeature&X=0&Y=0&width=100&height=100&INFO_FORMAT=text/html");
        LOGGER.info("WMS =GetFeatureInfo HTML response:\n" + prettyString(doc));
        assertXpathCount(1, "/html/body/table/tr/td[.='mf2']", doc);
        assertXpathCount(
                1,
                "/html/body/table/tr/td/table[caption/.='CGI_TermValuePropertyType']/tr/td/table[caption/.='CGI_TermValueType']",
                doc);
        assertXpathCount(
                1,
                "/html/body/table/tr/td/table[caption/.='GeologicFeaturePropertyType']/tr/td/table[caption/.='GeologicUnitType']/tr/th[.='gml:description']",
                doc);
    }
}
