/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import net.sf.json.JSON;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.filters.SpringDelegatingFilter;
import org.geoserver.proxybase.ext.config.ProxyBaseExtRuleDAO;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** Base class for tests that need to setup the Proxy Base Extension rules */
public class ProxyBaseExtensionTestSupport extends GeoServerSystemTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        new File(
                        testData.getDataDirectoryRoot(),
                        ProxyBaseExtRuleDAO.PROXY_BASE_EXT_RULES_DIRECTORY)
                .mkdir();
        testData.copyTo(
                ProxyBaseExtensionIntegrationTest.class.getResourceAsStream("/proxy-base-ext.xml"),
                ProxyBaseExtRuleDAO.PROXY_BASE_EXT_RULES_PATH);
    }

    @Override
    protected List<Filter> getFilters() {
        // needed so that the HTTPHeadersCollector Filter is initialized
        try {
            SpringDelegatingFilter filter = new SpringDelegatingFilter();
            filter.init(null);
            return Collections.singletonList(filter);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
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
