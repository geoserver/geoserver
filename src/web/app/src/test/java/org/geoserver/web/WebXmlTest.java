/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.geotools.util.NullEntityResolver;
import org.geotools.util.factory.Hints;
import org.geotools.xml.XMLUtils;
import org.junit.Test;

public class WebXmlTest {

    @Test
    public void testWebXmlXsdCompliance() throws Exception {
        // makes sure web.xml is XSD compliant (without requiring internet access in the process)
        // NullEntityResolver is safe here since every schema is a local, vendored file
        Hints hints = new Hints(Hints.ENTITY_RESOLVER, NullEntityResolver.INSTANCE);
        SchemaFactory factory = XMLUtils.newSchemaFactory(XMLConstants.W3C_XML_SCHEMA_NS_URI, hints);
        Schema schema = factory.newSchema(new Source[] {
            new StreamSource(new File("src/test/resources/xml.xsd")),
            new StreamSource(new File("src/test/resources/web-app_6_1.xsd")),
            new StreamSource(new File("src/test/resources/web-common_6_1.xsd")),
            new StreamSource(new File("src/test/resources/jakartaee_11.xsd")),
            new StreamSource(new File("src/test/resources/jakartaee_web_services_client_2_0.xsd")),
            new StreamSource(new File("src/test/resources/jsp_4_0.xsd"))
        });
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new File("src/main/webapp/WEB-INF/web.xml")));
    }

    @Test
    public void testJettyWithWindowsPath() throws Exception {
        Server jettyServer = new Server();
        try {
            WebAppContext context = new WebAppContext();
            context.setBaseResourceAsString("E:/SomePath/src/main/webapp");
            context.setThrowUnavailableOnStartupException(true);
            jettyServer.setHandler(context);
            jettyServer.start();
        } catch (java.nio.file.InvalidPathException p) {
            fail("Jetty server is not parsing Windows paths correctly");
        } catch (IllegalArgumentException e) {
            // this exception is ok because we made up the path
        } catch (Exception e) {
            fail("Jetty server is failing to startup for another reason");
        } finally {
            try {
                jettyServer.stop();
            } catch (Exception e) {
                // Suppress or log cleanup errors so they don't mask the primary test failure
            }
        }
    }
}
