/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.junit.Test;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class ApiConfigurationSupportTest extends OGCApiTestSupport {

    @Test
    public void testUrlHelperType() {
        RequestMappingHandlerMapping mappingHandler =
                applicationContext.getBean(RequestMappingHandlerMapping.class);
        assertEquals(
                mappingHandler.getUrlPathHelper().getClass().getSimpleName(),
                "GeoServerUrlPathHelper");
    }

    @Test
    public void testConvertersContainsFreeMarker() {
        RequestMappingHandlerAdapter mappingHandlerAdapter =
                applicationContext.getBean(RequestMappingHandlerAdapter.class);
        assertNotEquals(
                0,
                mappingHandlerAdapter.getMessageConverters().stream()
                        .filter(c -> c instanceof FreemarkerHTMLMessageConverter)
                        .count());
    }
}
