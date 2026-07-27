/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

/** Default smoke tester: wait for WMS capabilities and validate the response. */
class DefaultPluginTester extends AbstractPluginTester {

    @Override
    protected void verifyStarted(TestContext context, StartupProbeResult probe) throws Exception {
        verifyCapabilitiesResponse(context, probe.bodyAsString(), parseXml(probe.body()), "WMS_Capabilities");
    }
}
