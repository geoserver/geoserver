/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSInfo;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Test for 3D Bounding Box with Simple Features
 *
 * @author Niels Charlier
 */
public class BoundingBox3DTest extends WFS20TestSupport {

    @Override
    protected void setUpInternal(SystemTestData dataDirectory) throws Exception {

        // temp hack until feature bounding for 3D is fixed.
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(false);
        getGeoServer().save(wfs);

        // add extra types
        dataDirectory.addVectorLayer(
                new QName(SystemTestData.SF_URI, "With3D", SystemTestData.SF_PREFIX),
                Collections.EMPTY_MAP,
                org.geoserver.wfs.v1_1.BoundingBox3DTest.class,
                getCatalog());
    }

    @Test
    public void testBBox1() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=2.0.0&typename=sf:With3D&bbox=-200,-200,0,200,200,50");
        assertGML32(doc);

        NodeList features = doc.getElementsByTagName("sf:With3D");
        assertEquals(1, features.getLength());

        assertEquals(
                features.item(0).getAttributes().getNamedItem("gml:id").getNodeValue(), "fid1");
    }

    @Test
    public void testBBox2() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=getfeature&service=wfs&version=2.0.0&typename=sf:With3D&bbox=-200,-200,50,200,200,100");
        assertGML32(doc);

        NodeList features = doc.getElementsByTagName("sf:With3D");
        assertEquals(1, features.getLength());

        assertEquals(
                features.item(0).getAttributes().getNamedItem("gml:id").getNodeValue(), "fid2");
    }
}
