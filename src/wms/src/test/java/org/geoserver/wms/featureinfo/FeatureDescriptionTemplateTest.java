/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.junit.Assert.*;

import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.TemplateUtils;
import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class FeatureDescriptionTemplateTest {

    @Test
    public void testTemplate() throws Exception {
        Configuration cfg = TemplateUtils.getSafeConfiguration();
        cfg.setObjectWrapper(new FeatureWrapper());
        cfg.setClassForTemplateLoading(FeatureTemplate.class, "");

        Template template = cfg.getTemplate("description.ftl");
        assertNotNull(template);

        // create some data
        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureType featureType =
                DataUtilities.createType(
                        "testType", "string:String,int:Integer,double:Double,geom:Point");

        SimpleFeature f =
                SimpleFeatureBuilder.build(
                        featureType,
                        new Object[] {
                            "three",
                            Integer.valueOf(3),
                            Double.valueOf(3.3),
                            gf.createPoint(new Coordinate(3, 3))
                        },
                        "fid.3");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        template.process(f, new OutputStreamWriter(output));
        // template.process(f, new OutputStreamWriter(System.out));

        // This generates the following:

        // <h4>testType</h4>

        // <ul class="textattributes">
        // <li><strong><span class="atr-name">string</span>:</strong> <span
        // class="atr-value">three</span></li>
        // <li><strong><span class="atr-name">int</span>:</strong> <span
        // class="atr-value">3</span></li>
        // <li><strong><span class="atr-name">double</span>:</strong> <span
        // class="atr-value">3.3</span></li>
        //
        // </ul>

        // TODO docbuilder cannot parse this? May expect encapsulation, which table did provide

        // DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        // Document document = docBuilder.parse(new ByteArrayInputStream(output.toByteArray()));

        // assertNotNull(document);

        // assertEquals("table", document.getDocumentElement().getNodeName());
    }

    // public void testFeatureCollection() throws Exception {
    // Configuration cfg = TemplateUtils.getSafeConfiguration();
    // cfg.setObjectWrapper(new FeatureWrapper());
    // cfg.setClassForTemplateLoading(FeatureDescriptionTemplate.class, "");
    //
    // Template template = cfg.getTemplate("description.ftl");
    // assertNotNull(template);
    //
    // //create some data
    // GeometryFactory gf = new GeometryFactory();
    // FeatureType featureType = DataUtilities.createType("testType",
    // "string:String,int:Integer,double:Double,geom:Point");
    //
    // DefaultFeature f = new DefaultFeature((DefaultFeatureType) featureType,
    // new Object[] {
    // "three", Integer.valueOf(3), new Double(3.3), gf.createPoint(new Coordinate(3, 3))
    // }, "fid.3") {
    // };
    // DefaultFeature f4 = new DefaultFeature((DefaultFeatureType) featureType,
    // new Object[] {
    // "four", Integer.valueOf(4), new Double(4.4), gf.createPoint(new Coordinate(4, 4))
    // }, "fid.4") {
    // };
    // SimpleFeatureCollection features = new DefaultFeatureCollection(null,null) {};
    // features.add( f );
    // features.add( f4 );
    //
    // template.process(features, new OutputStreamWriter( System.out ));
    //
    // }
}
