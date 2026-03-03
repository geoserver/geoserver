/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v1_1;

import java.util.Collections;
import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class WFS11MultiPolygonAsMultiSurfaceTest extends WFSTestSupport {

    public static final QName COUNTRIES = new QName("http://geoserver.org/ne", "countries", "ne");

    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        namespaces.put("ne", "http://geoserver.org/ne");
    }

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        data.addVectorLayer(COUNTRIES, Collections.emptyMap(), "countries.properties", getClass(), getCatalog());
    }

    @Test
    public void testGeometryConsistency() throws Exception {
        // 1. Check DescribeFeatureType
        Document dft =
                getAsDOM("wfs?request=DescribeFeatureType&version=1.1.0&service=wfs&typename=" + getLayerId(COUNTRIES));
        print(dft);

        XMLAssert.assertXpathExists("//xsd:element[@name='geom' and @type='gml:MultiSurfacePropertyType']", dft);
        XMLAssert.assertXpathNotExists("//xsd:element[@name='geom' and @type='gml:MultyPolygonPropertyType']", dft);

        // 2. Check GetFeature
        Document gf = getAsDOM("wfs?request=GetFeature&version=1.1.0&service=wfs&typename=" + getLayerId(COUNTRIES));
        print(gf);
        XMLAssert.assertXpathExists("//ne:countries/ne:geom/gml:MultiSurface", gf);
        XMLAssert.assertXpathNotExists("//ne:countries/ne:geom/gml:MultiPolygon", gf);
    }
}
