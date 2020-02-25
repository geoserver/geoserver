/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.codec.binary.Base64;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.test.GeoServerAbstractTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.codec.Hex;

public class GeoServerBasicAuthenticationFilterTest {
    public static final String USERNAME = "admin:";
    public static final String PASSWORD = "geoserver";
    private static final int NTHREADS = 8;
    private String expected;
    private GeoServerBasicAuthenticationFilter authenticationFilter;

    @Before
    public void setUp() throws Exception {
        authenticationFilter = createAuthenticationFilter();
        StringBuffer buff = new StringBuffer(PASSWORD);
        buff.append(":");
        buff.append(authenticationFilter.getName());
        MessageDigest digest = MessageDigest.getInstance("MD5");
        String digestString =
                new String(Hex.encode(digest.digest(buff.toString().getBytes("utf-8"))));
        expected = USERNAME + digestString;
    }

    @Test
    public void testMultiThreadGetCacheKey() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(NTHREADS);
        List<Future<Boolean>> list = new ArrayList<Future<Boolean>>();
        for (int i = 0; i < 600; i++) {
            Callable<Boolean> worker = new AuthenticationCallable(authenticationFilter);
            Future<Boolean> submit = executor.submit(worker);
            list.add(submit);
        }

        for (Future<Boolean> future : list) {
            future.get();
        }

        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
    }

    private GeoServerBasicAuthenticationFilter createAuthenticationFilter() {
        GeoServerBasicAuthenticationFilter authenticationFilter =
                new GeoServerBasicAuthenticationFilter();
        GeoServerSecurityManager sm = null;
        try {
            sm = new GeoServerSecurityManager(new GeoServerDataDirectory(new File("target")));
            authenticationFilter.setSecurityManager(sm);
            BasicAuthenticationFilterConfig config = new BasicAuthenticationFilterConfig();
            authenticationFilter.initializeFromConfig(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize authentication authenticationFilter.");
        }
        return authenticationFilter;
    }

    private MockHttpServletRequest createRequest() {
        MockHttpServletRequest request =
                new GeoServerAbstractTestSupport.GeoServerMockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setContextPath("/geoserver");
        request.setRemoteAddr("127.0.0.1");
        String token = "admin:" + PASSWORD;
        request.addHeader(
                "Authorization", "Basic " + new String(Base64.encodeBase64(token.getBytes())));
        return request;
    }

    private class AuthenticationCallable implements Callable<Boolean> {
        private GeoServerBasicAuthenticationFilter authenticationFilter;

        private AuthenticationCallable(GeoServerBasicAuthenticationFilter authenticationFilter) {
            this.authenticationFilter = authenticationFilter;
        }

        @Override
        public Boolean call() throws Exception {
            MockHttpServletRequest request = createRequest();
            String result = authenticationFilter.getCacheKey(request);
            Assert.assertEquals(expected, result);
            return true;
        }
    }
}
