/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.rest.catalog.WorkspaceController;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.converters.XStreamCatalogListConverter;
import org.geoserver.rest.converters.XStreamJSONMessageConverter;
import org.geoserver.rest.converters.XStreamXMLMessageConverter;
import org.geoserver.rest.resources.ResourceDirectoryInfoJSONConverter;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
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

    @Test
    public void testXMLRestConverters() {
        // reading is done with the same class
        assertEquals(
                asList(XStreamXMLMessageConverter.class),
                getReadingConverters(RestWrapper.class, MediaType.APPLICATION_XML));
        // writing has specialization...
        assertEquals(
                asList(XStreamXMLMessageConverter.class),
                getWritingConverters(RestWrapper.class, MediaType.APPLICATION_XML));
        assertEquals(
                asList(XStreamCatalogListConverter.XMLXStreamListConverter.class),
                getWritingConverters(RestListWrapper.class, MediaType.APPLICATION_XML));
    }

    @Test
    public void testJSONRestConverters() {
        assertEquals(
                asList(XStreamJSONMessageConverter.class),
                getReadingConverters(RestWrapper.class, MediaType.APPLICATION_JSON));
        // writing has specialization...
        assertEquals(
                asList(XStreamJSONMessageConverter.class),
                getWritingConverters(RestWrapper.class, MediaType.APPLICATION_JSON));
        assertEquals(
                asList(XStreamCatalogListConverter.JSONXStreamListConverter.class),
                getWritingConverters(RestListWrapper.class, MediaType.APPLICATION_JSON));
    }

    @Test
    public void testJSONDocumentConverters() {
        assertEquals(
                asList(MappingJackson2HttpMessageConverter.class),
                getReadingConverters(AbstractDocument.class, MediaType.APPLICATION_JSON));
        assertEquals(
                asList(MappingJackson2HttpMessageConverter.class),
                getWritingConverters(AbstractDocument.class, MediaType.APPLICATION_JSON));
    }

    @Test
    public void testReadWithControllerContext() {
        // give more context and ensure there is no overlap reading objects that would be posted
        // to the REST API (those do not extend from RestWrapper)
        List<Class<?>> workspaceConverters =
                getFilteredConverters(
                        c -> {
                            // mimic Spring lookup
                            if (c instanceof GenericHttpMessageConverter)
                                return ((GenericHttpMessageConverter<?>) c)
                                        .canRead(
                                                WorkspaceInfo.class,
                                                WorkspaceController.class,
                                                MediaType.APPLICATION_JSON);
                            else return c.canRead(WorkspaceInfo.class, MediaType.APPLICATION_JSON);
                        });
        assertEquals(asList(XStreamJSONMessageConverter.class), workspaceConverters);

        // now try an API controller instead
        List<Class<?>> messageConverters =
                getFilteredConverters(
                        c -> {
                            // mimic Spring lookup
                            if (c instanceof GenericHttpMessageConverter)
                                return ((GenericHttpMessageConverter<?>) c)
                                        .canRead(
                                                Message.class,
                                                HelloController.class,
                                                MediaType.APPLICATION_JSON);
                            else return c.canRead(Message.class, MediaType.APPLICATION_JSON);
                        });
        assertEquals(asList(MappingJackson2HttpMessageConverter.class), messageConverters);
    }

    private List<Class<?>> getReadingConverters(Class<?> target, MediaType mediaType) {
        // skip  ResourceDirectoryInfoJSONConverter for now, it is a special subclass of
        // XStreamJSONMessageConverter that is only used if the wrapper actually contains
        // a ResourceDirectoryInfo, otherwise delegates to its parent class
        return getFilteredConverters(
                c ->
                        c.canRead(target, mediaType)
                                && !(c instanceof ResourceDirectoryInfoJSONConverter));
    }

    private List<Class<?>> getWritingConverters(Class<?> target, MediaType mediaType) {
        // see above for the ResourceDirectoryInfoJSONConverter weirdness
        return getFilteredConverters(
                c ->
                        c.canWrite(target, mediaType)
                                && !(c instanceof ResourceDirectoryInfoJSONConverter));
    }

    private static List<Class<?>> getFilteredConverters(Predicate<HttpMessageConverter<?>> filter) {
        RequestMappingHandlerAdapter mappingHandlerAdapter =
                applicationContext.getBean(RequestMappingHandlerAdapter.class);
        return mappingHandlerAdapter.getMessageConverters().stream()
                .filter(filter)
                .map(c -> c.getClass())
                .collect(Collectors.toList());
    }
}
