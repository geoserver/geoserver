/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.File;
import java.io.FileInputStream;
import junit.framework.TestCase;
import org.custommonkey.xmlunit.Validator;
import org.geotools.util.URLs;
import org.xml.sax.InputSource;

public class WebXmlTest extends TestCase {

    public void testWebXmlDTDCompliance() throws Exception {
        // makes sure web.xml is DTD compliant (without requiring internet access in the process)
        InputSource is = new InputSource(new FileInputStream("src/main/webapp/WEB-INF/web.xml"));
        Validator v =
                new Validator(
                        is,
                        URLs.fileToUrl(new File("src/test/java/org/geoserver/web/web-app_2_3.dtd"))
                                .toString());
        assertTrue(v.isValid());
    }
}
