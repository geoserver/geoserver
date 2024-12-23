/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML;
import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.sf.json.JSONObject;
import org.geoserver.ows.Request;
import org.geoserver.ows.TestDispatcherCallback;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

/**
 * Test class for APIRequestInfo, uses custom callbacks to test the APIRequestInfo object (as it's available during the
 * request processing)
 */
public class APIRequestInfoTest extends GeoServerSystemTestSupport {

    @Before
    public void cleanupCallbacks() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();
        dispatcher.callbacks.removeIf(c -> c instanceof TestDispatcherCallback);
    }

    @Test
    public void testProducibleMediaTypes() throws Exception {
        testDuringOperationExecuted(ri -> {
            Collection<MediaType> mediaTypes = ri.getProducibleMediaTypes(Message.class, true);
            assertThat(mediaTypes, Matchers.hasItems(APPLICATION_JSON, APPLICATION_YAML));
        });
    }

    @Test
    public void testLandingPage() throws Exception {
        testDuringOperationExecuted(ri -> assertEquals("ogc/hello/v1", ri.getServiceLandingPage()));
    }

    @Test
    public void testService() throws Exception {
        testDuringOperationExecuted(ri -> {
            Service service = ri.getService();
            assertEquals("Hello", service.getId());
            assertThat(service.getService(), Matchers.instanceOf(HelloService.class));
        });
    }

    @Test
    public void testIsFormatRequested() throws Exception {
        testDuringOperationExecuted(ri -> {
            assertTrue(ri.isFormatRequested(APPLICATION_JSON, APPLICATION_JSON));
        });
    }

    @Test
    public void testQueryMap() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();
        dispatcher.callbacks.add(new TestDispatcherCallback() {
            @Override
            public Object operationExecuted(Request request, Operation operation, Object result) {
                APIRequestInfo ri = APIRequestInfo.get();
                assertThat(ri.getSimpleQueryMap(), Matchers.hasEntry("k1", "v1"));
                assertThat(ri.getSimpleQueryMap(), Matchers.hasEntry("k2", "v2"));

                return null;
            }
        });
        // check no exceptions have been thrown
        JSONObject json = (JSONObject) getAsJSON("ogc/hello/v1?k1=v1&k2=v2");
        assertEquals("Landing page", json.get("message"));
    }

    @Test
    public void getLinks() throws Exception {
        testDuringOperationExecuted(ri -> {
            List<Link> links =
                    ri.getLinksFor("ogc/hello/v1/echo", Message.class, "Message as ", "alternate", true, "test", null);
            assertEquals(4, links.size());

            // check the links are correctly built
            Set<String> mimeTypes = new HashSet<>();
            for (Link link : links) {
                assertEquals("alternate", link.getRel());
                assertEquals("test", link.getClassification());
                assertEquals("Message as " + link.getType(), link.getTitle());
                mimeTypes.add(link.getType());
            }
            assertThat(
                    mimeTypes,
                    Matchers.hasItems(
                            MediaType.APPLICATION_JSON_VALUE,
                            MediaType.TEXT_HTML_VALUE,
                            MediaType.TEXT_PLAIN_VALUE,
                            APPLICATION_YAML_VALUE));
        });
    }

    private APIDispatcher getAPIDispatcher() {
        return applicationContext.getBean(APIDispatcher.class);
    }

    private void testDuringOperationExecuted(Consumer<APIRequestInfo> consumer) throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();
        dispatcher.callbacks.add(new TestDispatcherCallback() {
            @Override
            public Object operationExecuted(Request request, Operation operation, Object result) {
                APIRequestInfo ri = APIRequestInfo.get();
                consumer.accept(ri);

                return null;
            }
        });
        // check no exceptions have been thrown
        JSONObject json = (JSONObject) getAsJSON("ogc/hello/v1/");
        assertEquals("Landing page", json.get("message"));
    }
}
