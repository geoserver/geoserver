/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.namespace.QName;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ReprojectionTest extends WFSTestSupport {
    private static final String TARGET_CRS_CODE = "EPSG:900913";
    public static QName NULL_GEOMETRIES =
            new QName(SystemTestData.CITE_URI, "NullGeometries", SystemTestData.CITE_PREFIX);
    public static QName GOOGLE =
            new QName(SystemTestData.CITE_URI, "GoogleFeatures", SystemTestData.CITE_PREFIX);
    static MathTransform tx;

    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {

        CoordinateReferenceSystem epsg4326 = CRS.decode(TARGET_CRS_CODE);
        CoordinateReferenceSystem epsg32615 = CRS.decode("EPSG:32615");

        tx = CRS.findMathTransform(epsg32615, epsg4326);
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);

        dataDirectory.addVectorLayer(
                NULL_GEOMETRIES, Collections.EMPTY_MAP, getClass(), getCatalog());
        Map<LayerProperty, Object> extra = new HashMap<LayerProperty, Object>();
        extra.put(LayerProperty.PROJECTION_POLICY, ProjectionPolicy.REPROJECT_TO_DECLARED);
        extra.put(LayerProperty.SRS, 900913);
        dataDirectory.addVectorLayer(GOOGLE, extra, getClass(), getCatalog());
    }

    @Test
    public void testGetFeatureGet() throws Exception {

        Document dom1 =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.0.0&typename="
                                + SystemTestData.POLYGONS.getLocalPart());
        Document dom2 =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.0.0&typename="
                                + SystemTestData.POLYGONS.getLocalPart()
                                + "&srsName="
                                + TARGET_CRS_CODE);

        //        print(dom1);
        //        print(dom2);

        runTest(dom1, dom2, tx);
    }

    @Test
    public void testGetFeatureGetAutoCRS() throws Exception {

        Document dom1 =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.0.0&typename="
                                + SystemTestData.POLYGONS.getLocalPart());
        Document dom2 =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.0.0&typename="
                                + SystemTestData.POLYGONS.getLocalPart()
                                + "&srsName=AUTO:42001,9001,-93,0");

        //        print(dom1);
        //        print(dom2);

        MathTransform tx =
                CRS.findMathTransform(
                        CRS.decode("EPSG:32615"), CRS.decode("AUTO:42001,9001,-93,0"));
        runTest(dom1, dom2, tx);
    }

    @Test
    public void testGetFeatureAutoCRSBBox() throws Exception {
        CoordinateReferenceSystem auto = CRS.decode("AUTO:42001,9001,-93,0");
        FeatureTypeInfo ftInfo =
                getCatalog().getFeatureTypeByName(getLayerId(SystemTestData.POLYGONS));
        ReferencedEnvelope nativeEnv = ftInfo.getFeatureSource(null, null).getBounds();
        ReferencedEnvelope reprojectedEnv = nativeEnv.transform(auto, true);

        Document dom1 =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.0.0&typename="
                                + SystemTestData.POLYGONS.getLocalPart());
        Document dom2 =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.0.0&typename="
                                + SystemTestData.POLYGONS.getLocalPart()
                                + "&srsName=AUTO:42001,9001,-93,00&bbox="
                                + reprojectedEnv.getMinX()
                                + ","
                                + reprojectedEnv.getMinY()
                                + ","
                                + reprojectedEnv.getMaxX()
                                + ","
                                + reprojectedEnv.getMaxY()
                                + ",AUTO:42001,9001,-93,0");

        //            print(dom1);
        //            print(dom2);

        MathTransform tx = CRS.findMathTransform(CRS.decode("EPSG:32615"), auto);
        runTest(dom1, dom2, tx);
    }

    @Test
    public void testGetFeatureReprojectedFeatureType() throws Exception {
        // bbox is 4,4,6,6 in wgs84, coordinates have been reprojected to 900913
        Document dom =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=1.0.0&typename="
                                + GOOGLE.getLocalPart()
                                + "&bbox=445000,445000,668000,668000");
        print(dom);
        assertXpathEvaluatesTo("1", "count(//cite:GoogleFeatures)", dom);
    }

    @Test
    public void testGetFeaturePost() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\""
                        + SystemTestData.POLYGONS.getPrefix()
                        + ":"
                        + SystemTestData.POLYGONS.getLocalPart()
                        + "\"> "
                        + "<wfs:PropertyName>cgf:polygonProperty</wfs:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document dom1 = postAsDOM("wfs", xml);

        xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query srsName=\""
                        + TARGET_CRS_CODE
                        + "\" typeName=\""
                        + SystemTestData.POLYGONS.getPrefix()
                        + ":"
                        + SystemTestData.POLYGONS.getLocalPart()
                        + "\"> "
                        + "<wfs:PropertyName>cgf:polygonProperty</wfs:PropertyName> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        Document dom2 = postAsDOM("wfs", xml);

        runTest(dom1, dom2, tx);
    }

    @Test
    public void testReprojectNullGeometries() throws Exception {
        // see https://osgeo-org.atlassian.net/browse/GEOS-1612
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" "
                        + "version=\"1.0.0\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query srsName=\""
                        + TARGET_CRS_CODE
                        + "\" typeName=\""
                        + NULL_GEOMETRIES.getPrefix()
                        + ":"
                        + NULL_GEOMETRIES.getLocalPart()
                        + "\"> "
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        Document dom = postAsDOM("wfs", xml);
        //        print(dom);
        assertEquals(1, dom.getElementsByTagName("wfs:FeatureCollection").getLength());
    }

    @Test
    public void testGetFeatureWithProjectedBoxGet() throws Exception {
        Document dom;
        double[] cr = getTransformedPolygonsLayerBBox();

        String q =
                "wfs?request=getfeature&service=wfs&version=1.0&typeName="
                        + SystemTestData.POLYGONS.getLocalPart()
                        + "&bbox="
                        + cr[0]
                        + ","
                        + cr[1]
                        + ","
                        + cr[2]
                        + ","
                        + cr[3]
                        + ","
                        + TARGET_CRS_CODE;
        dom = getAsDOM(q);

        assertEquals(
                1,
                dom.getElementsByTagName(
                                SystemTestData.POLYGONS.getPrefix()
                                        + ":"
                                        + SystemTestData.POLYGONS.getLocalPart())
                        .getLength());
    }

    @Test
    public void testGetFeatureWithProjectedBoxPost() throws Exception {
        Document dom;
        double[] cr = getTransformedPolygonsLayerBBox();

        String xml =
                "<wfs:GetFeature service=\"WFS\" version=\"1.0.0\""
                        + " xmlns:"
                        + SystemTestData.POLYGONS.getPrefix()
                        + "=\""
                        + SystemTestData.POLYGONS.getNamespaceURI()
                        + "\""
                        + " xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + " xmlns:gml=\"http://www.opengis.net/gml\" "
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\""
                        + SystemTestData.POLYGONS.getPrefix()
                        + ":"
                        + SystemTestData.POLYGONS.getLocalPart()
                        + "\">"
                        + "<wfs:PropertyName>cgf:polygonProperty</wfs:PropertyName> "
                        + "<ogc:Filter>"
                        + "<ogc:BBOX>"
                        + "<ogc:PropertyName>polygonProperty</ogc:PropertyName>"
                        + "<gml:Box srsName=\""
                        + TARGET_CRS_CODE
                        + "\">"
                        + "<gml:coord>"
                        + "<gml:X>"
                        + cr[0]
                        + "</gml:X>"
                        + "<gml:Y>"
                        + cr[1]
                        + "</gml:Y>"
                        + "</gml:coord>"
                        + "<gml:coord>"
                        + "<gml:X>"
                        + cr[2]
                        + "</gml:X>"
                        + "<gml:Y>"
                        + cr[3]
                        + "</gml:Y>"
                        + "</gml:coord>"
                        + "</gml:Box>"
                        + "</ogc:BBOX>"
                        + "</ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        dom = postAsDOM("wfs", xml);

        assertEquals(
                1,
                dom.getElementsByTagName(
                                SystemTestData.POLYGONS.getPrefix()
                                        + ":"
                                        + SystemTestData.POLYGONS.getLocalPart())
                        .getLength());
    }

    /** See GEOT-3760 */
    @Test
    public void testGetFeatureWithProjectedBoxIntersectsPost() throws Exception {
        Document dom;
        double[] cr = getTransformedPolygonsLayerBBox();

        String xml =
                "<wfs:GetFeature service=\"WFS\" version=\"1.0.0\""
                        + " xmlns:"
                        + SystemTestData.POLYGONS.getPrefix()
                        + "=\""
                        + SystemTestData.POLYGONS.getNamespaceURI()
                        + "\""
                        + " xmlns:ogc=\"http://www.opengis.net/ogc\" "
                        + " xmlns:gml=\"http://www.opengis.net/gml\" "
                        + " xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "> "
                        + "<wfs:Query typeName=\""
                        + SystemTestData.POLYGONS.getPrefix()
                        + ":"
                        + SystemTestData.POLYGONS.getLocalPart()
                        + "\" srsName=\""
                        + TARGET_CRS_CODE
                        + "\">"
                        + "<wfs:PropertyName>cgf:polygonProperty</wfs:PropertyName> "
                        + "<ogc:Filter>"
                        + "<ogc:Intersects>"
                        + "<ogc:PropertyName>polygonProperty</ogc:PropertyName>"
                        + "<gml:Box>"
                        + "<gml:coord>"
                        + "<gml:X>"
                        + cr[0]
                        + "</gml:X>"
                        + "<gml:Y>"
                        + cr[1]
                        + "</gml:Y>"
                        + "</gml:coord>"
                        + "<gml:coord>"
                        + "<gml:X>"
                        + cr[2]
                        + "</gml:X>"
                        + "<gml:Y>"
                        + cr[3]
                        + "</gml:Y>"
                        + "</gml:coord>"
                        + "</gml:Box>"
                        + "</ogc:Intersects>"
                        + "</ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";

        dom = postAsDOM("wfs", xml);

        assertEquals(
                1,
                dom.getElementsByTagName(
                                SystemTestData.POLYGONS.getPrefix()
                                        + ":"
                                        + SystemTestData.POLYGONS.getLocalPart())
                        .getLength());
    }

    /** Returns the transformed corners of the POLYGON layer bbox */
    private double[] getTransformedPolygonsLayerBBox() throws Exception, TransformException {
        String q =
                "wfs?request=getfeature&service=wfs&version=1.0&typeName="
                        + SystemTestData.POLYGONS.getLocalPart();
        Document dom = getAsDOM(q);
        Element envelope = getFirstElementByTagName(dom, "gml:Box");
        String coordinates =
                getFirstElementByTagName(envelope, "gml:coordinates")
                        .getFirstChild()
                        .getNodeValue();
        String lc = coordinates.split(" ")[0];
        String uc = coordinates.split(" ")[1];
        double[] c =
                new double[] {
                    Double.parseDouble(lc.split(",")[0]), Double.parseDouble(lc.split(",")[1]),
                    Double.parseDouble(uc.split(",")[0]), Double.parseDouble(uc.split(",")[1])
                };
        double[] cr = new double[4];
        tx.transform(c, 0, cr, 0, 2);
        return cr;
    }

    private void runTest(Document dom1, Document dom2, MathTransform tx) throws Exception {
        Element box = getFirstElementByTagName(dom1.getDocumentElement(), "gml:Box");
        Element coordinates = getFirstElementByTagName(box, "gml:coordinates");
        double[] d1 = coordinates(coordinates.getFirstChild().getNodeValue());

        box = getFirstElementByTagName(dom2.getDocumentElement(), "gml:Box");
        coordinates = getFirstElementByTagName(box, "gml:coordinates");
        double[] d2 = coordinates(coordinates.getFirstChild().getNodeValue());

        double[] d3 = new double[d1.length];
        tx.transform(d1, 0, d3, 0, d1.length / 2);

        for (int i = 0; i < d2.length; i++) {
            assertEquals(d2[i], d3[i], 0.001);
        }
    }

    private double[] coordinates(String string) {
        StringTokenizer st = new StringTokenizer(string, " ");
        double[] coordinates = new double[st.countTokens() * 2];
        int i = 0;
        while (st.hasMoreTokens()) {
            String tuple = st.nextToken();
            coordinates[i++] = Double.parseDouble(tuple.split(",")[0]);
            coordinates[i++] = Double.parseDouble(tuple.split(",")[1]);
        }

        return coordinates;
    }
}
