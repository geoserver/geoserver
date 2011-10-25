package org.geoserver.wfs.v2_0;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.wfs.GetFeatureTest;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;
import org.w3c.dom.Document;

public class ExtendedOperatorTest extends WFS20TestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new ExtendedOperatorTest());
    }

    
    public void testInvokeExtendedOperator() throws Exception {
        
        String xml = 
            "<wfs:GetFeature service='WFS' version='2.0.0' " + 
               "xmlns:wfs='http://www.opengis.net/wfs/2.0' " +
               "xmlns:fes='http://www.opengis.net/fes/2.0' " +
               "xmlns:foo='http://foo.org'> " + 
                "<wfs:Query typeNames='sf:PrimitiveGeoFeature'> " +
                "  <fes:Filter>" +
                "   <foo:strMatches>" + 
                "     <fes:ValueReference>name</fes:ValueReference>" +
                "     <fes:Literal>name-f002</fes:Literal>" +
                "   </foo:strMatches>" + 
                "  </fes:Filter>" + 
                "</wfs:Query> " + 
              "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//sf:PrimitiveGeoFeature)", doc);
        XMLAssert.assertXpathExists("//sf:PrimitiveGeoFeature/gml:name[text()='name-f002']", doc);
    }
}
