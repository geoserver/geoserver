package org.geoserver.smartdataloader.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SmartDataLoaderDataAccessFactoryTest extends GeoServerSystemTestSupport {

    @Before
    public void setup() {
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "true");
        System.setProperty("TEST_PARAM", "resolved_value");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }

    /** Test that the factory can resolve the GeoServer environment properties from the parameters map */
    @Test
    public void testResolveParams() {
        String placeholderParam = "${TEST_PARAM}";
        Map<String, Serializable> params = new HashMap<>();
        params.put("param1", placeholderParam);
        Map<String, Serializable> resolvedParams = SmartDataLoaderDataAccessFactory.resolveParams(params);
        Assert.assertEquals("resolved_value", resolvedParams.get("param1"));
    }

    @After
    public void shutdown() {
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "false");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }
}
