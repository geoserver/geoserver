/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.File;
import javax.xml.transform.stream.StreamSource;
import junit.framework.TestCase;
import org.custommonkey.xmlunit.jaxp13.Validator;

public class WebXmlTest extends TestCase {

    public void testWebXmlXSZDCompliance() throws Exception {
        String xsd = "src/test/java/org/geoserver/web/";
        String web = "src/main/webapp/WEB-INF/web.xml";

        Validator v = new Validator();
        v.addSchemaSource(new StreamSource(new File(xsd + "web-app_3_1.xsd")));
        v.addSchemaSource(new StreamSource(new File(xsd + "web-common_3_1.xsd")));
        v.addSchemaSource(new StreamSource(new File(xsd + "javaee_7.xsd")));
        v.addSchemaSource(new StreamSource(new File(xsd + "javaee_web_services_client_1_4.xsd")));
        v.addSchemaSource(new StreamSource(new File(xsd + "jsp_2_3.xsd")));
        v.addSchemaSource(new StreamSource(new File(xsd + "xml.xsd")));

        StreamSource is = new StreamSource(new File(web));
        assertTrue("web", v.isInstanceValid(is));
    }
}
