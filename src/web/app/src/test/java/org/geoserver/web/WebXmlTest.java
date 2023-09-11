/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.File;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.junit.Test;

public class WebXmlTest {

    @Test
    public void testWebXmlXsdCompliance() throws Exception {
        // makes sure web.xml is XSD compliant (without requiring internet access in the process)
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema =
                factory.newSchema(
                        new Source[] {
                            new StreamSource(new File("src/test/resources/xml.xsd")),
                            new StreamSource(new File("src/test/resources/web-app_3_1.xsd")),
                            new StreamSource(new File("src/test/resources/web-common_3_1.xsd")),
                            new StreamSource(new File("src/test/resources/javaee_7.xsd")),
                            new StreamSource(
                                    new File(
                                            "src/test/resources/javaee_web_services_client_1_4.xsd")),
                            new StreamSource(new File("src/test/resources/jsp_2_3.xsd"))
                        });
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new File("src/main/webapp/WEB-INF/web.xml")));
    }
}
