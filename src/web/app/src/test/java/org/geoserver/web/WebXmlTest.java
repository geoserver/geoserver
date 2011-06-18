package org.geoserver.web;

import java.io.File;
import java.io.FileInputStream;

import junit.framework.TestCase;

import org.custommonkey.xmlunit.Validator;
import org.xml.sax.InputSource;

public class WebXmlTest extends TestCase {

    public void testWebXmlDTDCompliance() throws Exception {
        // makes sure web.xml is DTD compliant (without requiring internet access in the process)
        InputSource is = new InputSource(new FileInputStream("src/main/webapp/WEB-INF/web.xml"));
        Validator v = new Validator(is, new File("src/test/java/org/geoserver/web/web-app_2_3.dtd").toURL().toString());
        assertTrue(v.isValid());
    }
}
