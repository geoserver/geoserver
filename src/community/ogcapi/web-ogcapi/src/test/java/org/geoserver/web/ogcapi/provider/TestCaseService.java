package org.geoserver.web.ogcapi.provider;

import org.geoserver.ogcapi.APIService;

@APIService(
        service = "testCaseService",
        version = "2.3.4",
        landingPage = "ogc/TestCaseService/v1",
        serviceClass = TestCaseInfo.class)
public class TestCaseService {}
