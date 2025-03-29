/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

/** Used to filter out CapabilitiesHomePagePanelTest fake services. */
public class FakeServiceDescriptionProvider extends ServiceDescriptionProvider {
    public static String SERVICE_TYPE = "Fake";

    FakeServiceDescriptionProvider(String serviceType) {
        super(SERVICE_TYPE);
    }
}
