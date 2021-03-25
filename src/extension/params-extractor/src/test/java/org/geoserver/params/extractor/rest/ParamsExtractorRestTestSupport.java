/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.params.extractor.rest;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSON;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public abstract class ParamsExtractorRestTestSupport extends GeoServerSystemTestSupport {
    protected static XpathEngine xp;

    @Override
    protected void setUpTestData(SystemTestData testData) {
        // no data to setup

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("atom", "http://www.w3.org/2005/Atom");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();
    }

    public Document getAsDom(String path, String contentType, int status) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(status, response.getStatus());
        assertEquals(contentType, response.getContentType());
        return dom(new ByteArrayInputStream(response.getContentAsByteArray()));
    }

    public JSON getAsJSON(String path, String contentType, int status) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(status, response.getStatus());
        assertEquals(contentType, response.getContentType());
        return json(response);
    }
}
