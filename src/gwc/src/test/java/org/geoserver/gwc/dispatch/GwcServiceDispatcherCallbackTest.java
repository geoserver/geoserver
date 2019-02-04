/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.junit.After;
import org.junit.Test;

public final class GwcServiceDispatcherCallbackTest {

    @After
    public void cleanLocalWorkspace() {
        // clean any set local workspace
        LocalWorkspace.remove();
    }

    @Test
    public void testThatGwcServiceRequestsAreAccepted() {
        // creating some mocks needed for the test and instantiating the dispatcher call back
        HttpServletRequest httpRequest = newMockHttpRequest();
        when(httpRequest.getParameterMap()).thenReturn(Collections.emptyMap());
        Request request = mock(Request.class);
        when(request.getHttpRequest()).thenReturn(httpRequest);
        // the catalog will not be used so it can be null
        GwcServiceDispatcherCallback callback = new GwcServiceDispatcherCallback(null);
        // not a gwc request
        when(request.getContext()).thenReturn("wms/service");
        assertThat(callback.init(request), nullValue());
        // a simple gwc request
        when(request.getContext()).thenReturn("gwc/service");
        assertThat(callback.init(request), notNullValue());
        // a valid virtual service request (setting a local workspace will make the workspace valid)
        LocalWorkspace.set(mock(WorkspaceInfo.class));
        when(request.getContext()).thenReturn("validWorkspace/gwc/service");
        assertThat(callback.init(request), notNullValue());
        // an invalid virtual service request (a missing local workspace will make the workspace
        // invalid)
        LocalWorkspace.remove();
        when(request.getContext()).thenReturn("invalidWorkspace/gwc/service");
        try {
            callback.init(request);
            fail("The workspace is not valid, an exception should have been throw.");
        } catch (ServiceException serviceException) {
            assertThat(serviceException.getMessage(), is("No such workspace 'invalidWorkspace'"));
        }
    }

    public HttpServletRequest newMockHttpRequest() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("http");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/geoserver/gwc");
        return request;
    }

    @Test
    public void testGwcVirtualServiceRequestWrapper() {
        // we create a mock for the http request
        HttpServletRequest httpRequest = newMockHttpRequest();
        when(httpRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(httpRequest.getContextPath()).thenReturn("geoserver");
        // we create a mock for the geoserver request
        Request request = new Request();
        request.setKvp(Collections.singletonMap("LAYER", "someLayer"));
        request.setHttpRequest(httpRequest);
        request.setContext("someWorkspace/gwc/service");
        // mock for the local workspace
        WorkspaceInfo workspace = mock(WorkspaceInfo.class);
        when(workspace.getName()).thenReturn("someWorkspace");
        // instantiating the dispatcher callback
        GwcServiceDispatcherCallback callback = new GwcServiceDispatcherCallback(null);
        // setting a local workspace
        LocalWorkspace.set(workspace);
        Request wrappedRequest = callback.init(request);
        assertThat(wrappedRequest, notNullValue());
        assertThat(wrappedRequest.getHttpRequest(), notNullValue());
        assertThat(wrappedRequest.getHttpRequest().getContextPath(), is("geoserver/someWorkspace"));
        assertThat(
                wrappedRequest.getHttpRequest().getParameter("layer"),
                is("someWorkspace:someLayer"));
        assertThat(wrappedRequest.getHttpRequest().getParameterMap(), notNullValue());
        assertThat(
                wrappedRequest.getHttpRequest().getParameterMap().get("layer"),
                is(new String[] {"someWorkspace:someLayer"}));
        assertThat(
                wrappedRequest.getHttpRequest().getParameterValues("layer"),
                is(new String[] {"someWorkspace:someLayer"}));
    }

    @Test
    public void testThatGwcOperationIsStored() {
        // creating some mocks needed for the test and instantiating the dispatcher call back
        HttpServletRequest httpRequest = newMockHttpRequest();
        when(httpRequest.getParameterMap()).thenReturn(Collections.emptyMap());
        Request request = new Request();
        request.setKvp(Collections.singletonMap("REQUEST", "GetCapabilities"));
        request.setHttpRequest(httpRequest);
        request.setContext("gwc/service");
        // the catalog will not be used so it can be null
        GwcServiceDispatcherCallback callback = new GwcServiceDispatcherCallback(null);
        // invoke the dispatcher callback
        Request wrappedRequest = callback.init(request);
        assertThat(wrappedRequest, notNullValue());
        assertThat(GwcServiceDispatcherCallback.GWC_OPERATION.get(), notNullValue());
        assertThat(GwcServiceDispatcherCallback.GWC_OPERATION.get(), is("GetCapabilities"));
    }
}
