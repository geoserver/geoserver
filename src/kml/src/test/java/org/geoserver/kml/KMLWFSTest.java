package org.geoserver.kml;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class KMLWFSTest extends WFSTestSupport {
    
    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");
    }
    

    @Test
    public void testGetCapabilities() throws Exception {
        Document doc = getAsDOM("wfs?request=GetCapabilities&version=1.1.0");
        // print(doc);

        // check the new output format is part of the caps document
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ows:Operation[@name='GetFeature']/"
                + "ows:Parameter[@name='outputFormat']/ows:Value[text() = '"
                + KMLMapOutputFormat.MIME_TYPE + "'])", doc);
    }

    @Test
    public void testGetFeature() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wfs?service=WFS&version=1.1.0&request=GetFeature&typeName="
                + getLayerId(MockData.AGGREGATEGEOFEATURE) + "&outputFormat="
                + KMLMapOutputFormat.MIME_TYPE.replace("+", "%2B"));
        assertEquals(200, response.getStatusCode());
        assertEquals("inline; filename=" + MockData.AGGREGATEGEOFEATURE.getLocalPart() + ".kml", response.getHeader("Content-Disposition"));
        Document doc = dom(new ByteArrayInputStream( response.getOutputStreamContent().getBytes()));
        checkAggregateGeoFeatureKmlContents(doc);
    }
    
    @Test
    public void testGetFeatureKMLAlias() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&version=1.1.0&request=GetFeature&typeName="
                + getLayerId(MockData.AGGREGATEGEOFEATURE) + "&outputFormat=KML");
        checkAggregateGeoFeatureKmlContents(doc);
    }


    private void checkAggregateGeoFeatureKmlContents(Document doc) throws Exception {
        // print(doc);
        
        // there is one schema
        XMLAssert.assertXpathEvaluatesTo("1", "count(//kml:Document/kml:Schema)", doc);
        // check we only have the non geom properties
        XMLAssert.assertXpathEvaluatesTo("6", "count(//kml:Document/kml:Schema/kml:SimpleField)",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                        "0",
                        "count(//kml:Document/kml:Schema/kml:SimpleField[@name='multiPointProperty'])",
                        doc);
        XMLAssert.assertXpathEvaluatesTo(
                        "0",
                        "count(//kml:Document/kml:Schema/kml:SimpleField[@name='multiCurveProperty'])",
                        doc);
        XMLAssert.assertXpathEvaluatesTo("0",
                "count(//kml:Document/kml:Schema/kml:SimpleField[@name='multiSurfaceProperty'])",
                doc);
        // check the type mapping
        XMLAssert.assertXpathEvaluatesTo("string",
                "//kml:Document/kml:Schema/kml:SimpleField[@name='description']/@type", doc);
        XMLAssert.assertXpathEvaluatesTo("double",
                "//kml:Document/kml:Schema/kml:SimpleField[@name='doubleProperty']/@type", doc);
        XMLAssert.assertXpathEvaluatesTo("int",
                "//kml:Document/kml:Schema/kml:SimpleField[@name='intRangeProperty']/@type", doc);
        XMLAssert.assertXpathEvaluatesTo("string",
                "//kml:Document/kml:Schema/kml:SimpleField[@name='strProperty']/@type", doc);
        XMLAssert.assertXpathEvaluatesTo("string",
                "//kml:Document/kml:Schema/kml:SimpleField[@name='featureCode']/@type", doc);

        // check the extended data of one feature
        String sd = "//kml:Placemark[@id='AggregateGeoFeature.f005']/kml:ExtendedData/kml:SchemaData/kml:SimpleData";
        XMLAssert.assertXpathEvaluatesTo("description-f005", sd + "[@name='description']", doc);
        XMLAssert.assertXpathEvaluatesTo("name-f005", sd + "[@name='name']", doc);
        XMLAssert.assertXpathEvaluatesTo("2012.78", sd + "[@name='doubleProperty']", doc);
        XMLAssert.assertXpathEvaluatesTo("Ma quande lingues coalesce, li grammatica del resultant "
                + "lingue es plu simplic e regulari quam ti del coalescent lingues. Li nov lingua "
                + "franca va esser plu simplic e regulari quam li existent Europan lingues.", sd
                + "[@name='strProperty']", doc);
        XMLAssert.assertXpathEvaluatesTo("BK030", sd + "[@name='featureCode']", doc);
    }
    
    @Test
    public void testForceWGS84() throws Exception {
        Document doc = getAsDOM("wfs?service=WFS&version=1.1.0&request=GetFeature&typeName="
                + getLayerId(MockData.MPOINTS) + "&outputFormat=KML");
                
        // print(doc);

        XMLAssert.assertXpathEvaluatesTo("1", "count(//kml:Folder)", doc);
        XMLAssert.assertXpathEvaluatesTo("-92.99707024070754,4.523788746085423", "//kml:Placemark/kml:MultiGeometry/kml:Point[1]/kml:coordinates", doc);
        XMLAssert.assertXpathEvaluatesTo("-92.99661950641159,4.524241081543828", "//kml:Placemark/kml:MultiGeometry/kml:Point[2]/kml:coordinates", doc);
    }
}
