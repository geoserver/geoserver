/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test for 3D BBOXes in App-schema Support for offline as well as online testing
 *
 * @author Niels Charlier
 */
public class BBox3DTest extends AbstractAppSchemaTestSupport {

    @Override
    protected BBox3DMockData createTestData() {
        return new BBox3DMockData();
    }

    /** Tests re-projection of NonFeatureTypeProxy. */
    @Test
    public void testBbox1() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&srsName=EPSG:4979&bbox=-200,-200,0,200,200,50");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        // all features fall in to the x-y boundaries, only mf2 and mf3 fall in to the z boundaries
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']", doc);
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']", doc);
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']", doc);
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']", doc);

        assertXpathEvaluatesTo(
                "167.9388 -29.0434 7",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4979",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsName",
                doc);
    }

    /** Tests re-projection of NonFeatureTypeProxy. */
    @Test
    public void testBbox2() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&srsName=EPSG:4979&bbox=-200,-200,50,200,200,200");
        // print(doc);
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        // all features fall in to the x-y boundaries, only mf1 and mf4 fall in to the z boundaries
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']", doc);
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']", doc);
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']", doc);
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']", doc);

        assertXpathEvaluatesTo(
                "133.8855 -23.6701 112",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4979",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsName",
                doc);
    }

    /** Tests re-projection of NonFeatureTypeProxy. */
    @Test
    public void testBboxPost() {
        String xml = //
                "<wfs:GetFeature " //
                        + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                        + ">" //
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                        + "        <ogc:Filter>" //
                        + "            <ogc:BBOX>" //
                        + "              <gml:Envelope srsName=\"EPSG:4979\"> "
                        + " 					<gml:lowerCorner>-200 -200 0 </gml:lowerCorner> "
                        + "						<gml:upperCorner> 200 200 50 </gml:upperCorner> "
                        + "              </gml:Envelope> "
                        + "           </ogc:BBOX>" //
                        + "        </ogc:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        // all features fall in to the x-y boundaries, only mf1 and mf4 fall in to the z boundaries
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']", doc);
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']", doc);
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']", doc);
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']", doc);
    }
}
