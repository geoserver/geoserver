package org.geoserver.web.security.oauth2.intgration.keycloak;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class DelmeTest extends GeoServerSystemTestSupport {

    @Test
    public void ttttt() throws Exception {
        MockHttpServletRequest request = createRequest("web/");
        request.setMethod("GET");
        MockHttpServletResponse sr = dispatch(request);
        var t = 0;
    }

    @Before
    public void before() {
        int t = 0;
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        int t = 0;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        int t = 0;
    }
}
