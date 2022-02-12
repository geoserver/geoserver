/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.wms.WMSInfo;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.MarkFactory;
import org.geotools.renderer.style.MarkFactoryListComparator;
import org.geotools.renderer.style.MarkFactoryListPredicate;
import org.geotools.util.logging.Logging;

/**
 * Checks current MarkFactory filter and order configuration on GeoServer global and per workspace
 * WMSinfo instances. Injects the required {@link Comparator} and {@link Predicate} into the {@link
 * RenderingHints} map.
 */
public class MarkFactoryHintsInjector {

    private static Logger LOGGER = Logging.getLogger(MarkFactoryHintsInjector.class);

    public static final String MARK_FACTORY_LIST = "MarkFactoryList";

    private final GeoServer geoServer;

    public MarkFactoryHintsInjector(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    /** Adds the filter and order hints to rendering process in base to workspace level WMS info. */
    public void addMarkFactoryHints(RenderingHints hints) {
        List<String> markFactoryList = markFactoryOrderByWorkspace();
        Predicate<MarkFactory> predicate = predicate(markFactoryList);
        if (predicate != null) hints.put(DynamicSymbolFactoryFinder.MARK_FACTORY_FILTER, predicate);
        Comparator<MarkFactory> comparator = comparator(markFactoryList);
        if (comparator != null) {
            hints.put(DynamicSymbolFactoryFinder.MARK_FACTORY_ORDER, comparator);
        }
    }

    private List<String> markFactoryOrderByWorkspace() {
        WMSInfo wmsInfo = null;
        WorkspaceInfo workspaceInfo = LocalWorkspace.get();
        if (workspaceInfo != null) {
            wmsInfo = geoServer.getService(workspaceInfo, WMSInfo.class);
        }
        if (wmsInfo == null) {
            wmsInfo = geoServer.getService(WMSInfo.class);
        }
        return getMarkFactoryList(wmsInfo);
    }

    private Predicate<MarkFactory> predicate(List<String> markFactoryList) {
        if (CollectionUtils.isEmpty(markFactoryList)) {
            return null;
        }
        return new MarkFactoryListPredicate(markFactoryList);
    }

    private Comparator<MarkFactory> comparator(List<String> markFactoryList) {
        if (CollectionUtils.isEmpty(markFactoryList)) {
            return null;
        }
        return new MarkFactoryListComparator(markFactoryList);
    }

    private List<String> getMarkFactoryList(WMSInfo wmsInfo) {
        if (wmsInfo.getMetadata().containsKey(MARK_FACTORY_LIST)) {
            String factoriesStr = wmsInfo.getMetadata().get(MARK_FACTORY_LIST, String.class);
            if (StringUtils.isNotBlank(factoriesStr)) {
                List<String> factoryNames = Arrays.asList(factoriesStr.split(","));
                if (validateIdentifiers(factoryNames)) {
                    LOGGER.log(
                            Level.FINE,
                            "Configured MarkFactory precedence found: {0}",
                            factoryNames);
                    return factoryNames;
                }
            }
        }
        LOGGER.log(Level.FINE, "Configured MarkFactory precedence not found");
        return Collections.emptyList();
    }

    private boolean validateIdentifiers(List<String> identifiers) {
        List<String> availableFactories =
                IteratorUtils.toList(DynamicSymbolFactoryFinder.getMarkFactories()).stream()
                        .map(mf -> mf.getClass().getSimpleName())
                        .collect(Collectors.toList());
        for (String identifier : identifiers) {
            if (!availableFactories.contains(identifier)) {
                LOGGER.log(
                        Level.SEVERE,
                        "The {0} mark factory class name identifier is not available on the classpath.",
                        identifier);
                return false;
            }
        }
        return true;
    }
}
