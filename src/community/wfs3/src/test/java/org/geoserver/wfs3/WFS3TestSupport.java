/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.JsonContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.CoreMatchers;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.Filter;
import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class WFS3TestSupport extends GeoServerSystemTestSupport {

    @Override
    protected List<Filter> getFilters() {
        return Collections.singletonList(new WFS3Filter(getCatalog()));
    }
    
    protected String getEncodedName(QName qName) {
        if(qName.getPrefix() != null) {
            return qName.getPrefix() + "__" + qName.getLocalPart();
        }
        else {
            return qName.getLocalPart();
        }
    }

    protected DocumentContext getAsJSONPath(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        

        assertEquals(expectedHttpCode, response.getStatus());
        assertThat(
                response.getContentType(), 
                anyOf(startsWith("application/json"), startsWith("application/geo+json")));
        JsonContext json = (JsonContext) JsonPath.parse(response.getContentAsString());
        if (!isQuietTests()) {
            print(json(response));
        }
        return json;
    }

    @Override
    protected void onSetUp(SystemTestData testData) {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wfs", "http://www.opengis.net/wfs/3.0");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");

        CiteTestData.registerNamespaces(namespaces);

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    
}
