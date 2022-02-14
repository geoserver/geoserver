/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.IteratorUtils;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.MarkFactory;
import org.geotools.util.factory.Hints;
import org.junit.Test;

public class MarkFactoryProcessorProviderFactoryTest extends WMSTestSupport {

    @Test
    public void testDefaultResult() throws Exception {
        Hints hints = new Hints();
        MarkFactoryHintsInjector injector = new MarkFactoryHintsInjector(getGeoServer());
        injector.addMarkFactoryHints(hints);

        List<MarkFactory> expectedFactories =
                IteratorUtils.toList(DynamicSymbolFactoryFinder.getMarkFactories());

        List<MarkFactory> processedFactories =
                IteratorUtils.toList(DynamicSymbolFactoryFinder.getMarkFactories(hints));
        assertEquals(expectedFactories, processedFactories);
    }

    private String toListString(List<String> list) {
        StringBuilder builder = new StringBuilder();
        boolean started = false;
        for (String value : list) {
            if (started) builder.append(",");
            builder.append(value);
            started = true;
        }
        return builder.toString();
    }

    @Test
    public void testGlobalFilteredResult() throws Exception {
        // create the desired mark factories and order
        ArrayList<String> markFactoryNames =
                new ArrayList<>(
                        Arrays.asList("WKTMarkFactory", "WellKnownMarkFactory", "TTFMarkFactory"));
        // set the filter and order on global wmsInfo
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        wmsInfo.getMetadata()
                .put(MarkFactoryHintsInjector.MARK_FACTORY_LIST, toListString(markFactoryNames));
        getGeoServer().save(wmsInfo);
        // test the result
        Hints hints = new Hints();
        MarkFactoryHintsInjector injector = new MarkFactoryHintsInjector(getGeoServer());
        injector.addMarkFactoryHints(hints);
        List<MarkFactory> processedFactories =
                IteratorUtils.toList(DynamicSymbolFactoryFinder.getMarkFactories(hints));
        assertEquals(
                Arrays.asList("WKTMarkFactory", "WellKnownMarkFactory", "TTFMarkFactory"),
                processedFactories.stream()
                        .map(mf -> mf.getClass().getSimpleName())
                        .collect(Collectors.toList()));
    }

    @Test
    public void testWorkspaceConfig() throws Exception {
        GeoServer geoServer = getGeoServer();
        WorkspaceInfo workspaceInfo = geoServer.getCatalog().getWorkspaceByName("cgf");
        try {
            // create the desired mark factories and order
            ArrayList<String> markFactoryNames =
                    new ArrayList<>(
                            Arrays.asList(
                                    "WKTMarkFactory", "WellKnownMarkFactory", "TTFMarkFactory"));
            // set the filter and order on global wmsInfo
            WMSInfoImpl wmsInfo = new WMSInfoImpl();
            wmsInfo.setId("wms");
            wmsInfo.setName("WMS");
            wmsInfo.setEnabled(true);
            wmsInfo.setWorkspace(workspaceInfo);
            wmsInfo.getMetadata()
                    .put(
                            MarkFactoryHintsInjector.MARK_FACTORY_LIST,
                            toListString(markFactoryNames));
            geoServer.add(wmsInfo);

            LocalWorkspace.set(workspaceInfo);
            Hints hints = new Hints();
            MarkFactoryHintsInjector injector = new MarkFactoryHintsInjector(getGeoServer());
            injector.addMarkFactoryHints(hints);
            List<MarkFactory> processedFactories =
                    IteratorUtils.toList(DynamicSymbolFactoryFinder.getMarkFactories(hints));
            assertEquals(
                    Arrays.asList("WKTMarkFactory", "WellKnownMarkFactory", "TTFMarkFactory"),
                    processedFactories.stream()
                            .map(mf -> mf.getClass().getSimpleName())
                            .collect(Collectors.toList()));
        } finally {
            WMSInfo wmsInfo = geoServer.getService(workspaceInfo, WMSInfo.class);
            geoServer.remove(wmsInfo);
            LocalWorkspace.remove();
        }
    }
}
