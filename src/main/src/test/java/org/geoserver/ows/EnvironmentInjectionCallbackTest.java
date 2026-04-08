/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Function;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EnvironmentInjectionCallbackTest extends GeoServerSystemTestSupport {

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory(null);

    private static final Function ENV_FUNCTION = FF.function("env", FF.literal("GSUSER"));

    private static final EnviromentInjectionCallback CALLBACK = new EnviromentInjectionCallback();

    private Request request = null;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpSecurity();
    }

    @Before
    public void init() {
        this.request = new Request();
        this.request.setKvp(new HashMap<>());
    }

    @After
    public void clearLocalValues() {
        CALLBACK.finished(this.request);
        logout();
    }

    @Test
    public void testAnonymousWithoutEnv() throws Exception {
        CALLBACK.init(this.request);
        assertNull(ENV_FUNCTION.evaluate(null));
    }

    @Test
    public void testAnonymousWithoutUserInEnv() throws Exception {
        this.request.getKvp().put("env", new HashMap<>());
        CALLBACK.init(this.request);
        assertNull(ENV_FUNCTION.evaluate(null));
    }

    @Test
    public void testAnonymousWithUserInEnv() throws Exception {
        this.request.getKvp().put("env", new HashMap<>(Map.of("GSUSER", "foo")));
        CALLBACK.init(this.request);
        assertNull(ENV_FUNCTION.evaluate(null));
    }

    @Test
    public void testAdminWithoutEnv() throws Exception {
        loginAsAdmin();
        CALLBACK.init(this.request);
        assertEquals("admin", ENV_FUNCTION.evaluate(null));
    }

    @Test
    public void testAdminWithoutUserInEnv() throws Exception {
        loginAsAdmin();
        this.request.getKvp().put("env", new HashMap<>());
        CALLBACK.init(this.request);
        assertEquals("admin", ENV_FUNCTION.evaluate(null));
    }

    @Test
    public void testAdminWithUserInEnv() throws Exception {
        loginAsAdmin();
        this.request.getKvp().put("env", new HashMap<>(Map.of("GSUSER", "foo")));
        CALLBACK.init(this.request);
        assertEquals("admin", ENV_FUNCTION.evaluate(null));
    }
}
