/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi.provider;

import org.geoserver.ogcapi.APIService;

@APIService(
        service = "testCaseService",
        version = "2.3.4",
        landingPage = "ogc/TestCaseService/v1",
        serviceClass = TestCaseInfo.class)
public class TestCaseService {}
